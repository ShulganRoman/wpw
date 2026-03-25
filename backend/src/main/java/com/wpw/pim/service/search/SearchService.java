package com.wpw.pim.service.search;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final JdbcTemplate jdbcTemplate;

    private static final String TRANSLATION_JOIN = """
            LEFT JOIN product_translations pt  ON pt.product_id = p.id AND pt.locale = ?
            LEFT JOIN product_translations pen ON pen.product_id = p.id AND pen.locale = 'en'
            """;

    private static final String NAME_EXPR = "COALESCE(pt.name, pen.name, '')";
    private static final String DESC_EXPR = "COALESCE(pt.short_description, pen.short_description, '')";

    public List<Map<String, Object>> search(String query, String locale, int page, int perPage) {
        int offset = (page - 1) * perPage;

        String ftsSql = """
            SELECT p.id, p.tool_no, %s AS name, %s AS short_description, p.status,
                   ts_rank(
                       to_tsvector('simple', %s || ' ' || %s || ' ' || p.tool_no),
                       plainto_tsquery('simple', ?)
                   ) AS rank
            FROM products p
            %s
            WHERE p.status = 'active'
              AND to_tsvector('simple', %s || ' ' || %s || ' ' || p.tool_no)
                  @@ plainto_tsquery('simple', ?)
            ORDER BY rank DESC, p.tool_no
            LIMIT ? OFFSET ?
            """.formatted(NAME_EXPR, DESC_EXPR, NAME_EXPR, DESC_EXPR, TRANSLATION_JOIN, NAME_EXPR, DESC_EXPR);

        List<Map<String, Object>> results = jdbcTemplate.queryForList(ftsSql, query, locale, query, perPage, offset);
        if (!results.isEmpty()) return results;

        String likeSql = """
            SELECT p.id, p.tool_no, %s AS name, %s AS short_description, p.status, 1.0 AS rank
            FROM products p
            %s
            WHERE p.status = 'active'
              AND (
                  p.tool_no ILIKE ?
                  OR %s ILIKE ?
                  OR %s ILIKE ?
              )
            ORDER BY p.tool_no
            LIMIT ? OFFSET ?
            """.formatted(NAME_EXPR, DESC_EXPR, TRANSLATION_JOIN, NAME_EXPR, DESC_EXPR);

        String pattern = "%" + query.replace("%", "\\%") + "%";
        return jdbcTemplate.queryForList(likeSql, locale, pattern, pattern, pattern, perPage, offset);
    }

    public long countSearch(String query, String locale) {
        String ftsSql = """
            SELECT COUNT(*)
            FROM products p
            %s
            WHERE p.status = 'active'
              AND to_tsvector('simple', %s || ' ' || %s || ' ' || p.tool_no)
                  @@ plainto_tsquery('simple', ?)
            """.formatted(TRANSLATION_JOIN, NAME_EXPR, DESC_EXPR);
        Long count = jdbcTemplate.queryForObject(ftsSql, Long.class, locale, query);
        if (count != null && count > 0) return count;

        String likeSql = """
            SELECT COUNT(*)
            FROM products p
            %s
            WHERE p.status = 'active'
              AND (
                  p.tool_no ILIKE ?
                  OR %s ILIKE ?
                  OR %s ILIKE ?
              )
            """.formatted(TRANSLATION_JOIN, NAME_EXPR, DESC_EXPR);
        String pattern = "%" + query.replace("%", "\\%") + "%";
        Long likeCount = jdbcTemplate.queryForObject(likeSql, Long.class, locale, pattern, pattern, pattern);
        return likeCount != null ? likeCount : 0;
    }
}
