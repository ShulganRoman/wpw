package com.wpw.pim.service.operation;

import com.wpw.pim.domain.operation.Operation;
import com.wpw.pim.repository.operation.OperationRepository;
import com.wpw.pim.service.product.ProductService;
import com.wpw.pim.web.dto.common.PagedResponse;
import com.wpw.pim.web.dto.operation.ApplicationTagUpsertDto;
import com.wpw.pim.web.dto.operation.OperationDto;
import com.wpw.pim.web.dto.product.ProductFilter;
import com.wpw.pim.web.dto.product.ProductSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class OperationService {

    private final OperationRepository operationRepository;
    private final ProductService productService;

    @Cacheable("operations")
    @Transactional(readOnly = true)
    public List<Operation> findAll() {
        return operationRepository.findAllByOrderBySortOrder();
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProductSummaryDto> findProductsByOperation(String code, String locale, int page, int perPage) {
        ProductFilter filter = new ProductFilter(locale, null, null, null, code, null, null, null, null, null,
            null, null, null, null, null, null, page, perPage);
        return productService.findAll(filter);
    }

    @CacheEvict(value = "operations", allEntries = true)
    @Transactional
    public OperationDto create(ApplicationTagUpsertDto dto) {
        String code = toCode(dto.name());
        if (operationRepository.existsById(code)) {
            throw new ResponseStatusException(CONFLICT, "Application tag already exists: " + code);
        }
        Operation op = new Operation();
        op.setCode(code);
        op.setName(dto.name().trim());
        op.setNameKey("op." + code);
        op.setSortOrder(dto.sortOrder() != null ? dto.sortOrder()
            : (int) operationRepository.count());
        return OperationDto.from(operationRepository.save(op));
    }

    @CacheEvict(value = "operations", allEntries = true)
    @Transactional
    public OperationDto update(String code, ApplicationTagUpsertDto dto) {
        Operation op = operationRepository.findById(code)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Application tag not found: " + code));
        if (dto.name() != null) op.setName(dto.name().trim());
        if (dto.sortOrder() != null) op.setSortOrder(dto.sortOrder());
        return OperationDto.from(operationRepository.save(op));
    }

    @CacheEvict(value = "operations", allEntries = true)
    @Transactional
    public void delete(String code) {
        if (!operationRepository.existsById(code)) {
            throw new ResponseStatusException(NOT_FOUND, "Application tag not found: " + code);
        }
        operationRepository.deleteById(code);
    }

    /** Converts a display name to a URL-safe code, e.g. "Surface Processing" → "surface-processing" */
    private static String toCode(String name) {
        return name.trim()
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("^-|-$", "");
    }
}
