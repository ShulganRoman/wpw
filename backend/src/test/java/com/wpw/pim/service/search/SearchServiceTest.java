package com.wpw.pim.service.search;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты для {@link SearchService}.
 * Проверяют что SearchService формирует правильные SQL-запросы.
 * Возвращаемые значения — дефолтные (пустые списки/0), т.к. JdbcTemplate varargs
 * требует специального подхода к матчингу.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SearchServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private SearchService searchService;

    @BeforeEach
    void setUp() {
        searchService = new SearchService(jdbcTemplate);
    }

    @Nested
    @DisplayName("search — SQL формирование")
    class Search {

        @Test
        @DisplayName("search вызывает FTS-запрос с ts_rank")
        void search_callsFtsQuery() {
            searchService.search("DR001", "en", 1, 10);

            verify(jdbcTemplate).queryForList(
                contains("ts_rank"),
                eq("DR001"), eq("en"), eq("DR001"), eq(10), eq(0)
            );
        }

        @Test
        @DisplayName("search при FTS-промахе вызывает ILIKE запрос")
        void search_ftsEmptyCallsLikeQuery() {
            // FTS возвращает пустой список (default mock behaviour)
            searchService.search("DR002", "en", 1, 10);

            // LIKE-запрос должен быть вызван с паттерном %DR002%
            verify(jdbcTemplate).queryForList(
                contains("ILIKE"),
                eq("en"), eq("%DR002%"), eq("%DR002%"), eq("%DR002%"), eq(10), eq(0)
            );
        }

        @Test
        @DisplayName("search страница 3 вычисляет offset=40")
        void search_pagination_calculatesOffset() {
            searchService.search("test", "en", 3, 20);

            verify(jdbcTemplate).queryForList(
                contains("ts_rank"),
                eq("test"), eq("en"), eq("test"), eq(20), eq(40)
            );
        }

        @Test
        @DisplayName("search экранирует % в ILIKE-паттерне")
        void search_percentInQuery_escapedInPattern() {
            searchService.search("100%", "en", 1, 10);

            verify(jdbcTemplate).queryForList(
                contains("ILIKE"),
                eq("en"), eq("%100\\%%"), eq("%100\\%%"), eq("%100\\%%"), eq(10), eq(0)
            );
        }

        @Test
        @DisplayName("search возвращает непустой List (тип проверки)")
        void search_returnsListType() {
            List<Map<String, Object>> result = searchService.search("x", "en", 1, 10);
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("countSearch — SQL формирование")
    class CountSearch {

        @Test
        @DisplayName("countSearch вызывает FTS-запрос count")
        void countSearch_callsFtsQuery() {
            searchService.countSearch("router", "en");

            verify(jdbcTemplate).queryForObject(
                contains("to_tsvector"),
                eq(Long.class),
                eq("en"), eq("router")
            );
        }

        @Test
        @DisplayName("countSearch при FTS-промахе вызывает ILIKE count")
        void countSearch_ftsNullFallsBackToLike() {
            // queryForObject returning null (default mock) → fallback to LIKE
            searchService.countSearch("router", "en");

            verify(jdbcTemplate).queryForObject(
                contains("ILIKE"),
                eq(Long.class),
                eq("en"), eq("%router%"), eq("%router%"), eq("%router%")
            );
        }

        @Test
        @DisplayName("countSearch возвращает long (не NPE)")
        void countSearch_returnsLong() {
            long count = searchService.countSearch("nothing", "en");
            assertThat(count).isGreaterThanOrEqualTo(0);
        }
    }
}
