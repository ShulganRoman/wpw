package com.wpw.pim.service.operation;

import com.wpw.pim.domain.operation.Operation;
import com.wpw.pim.repository.operation.OperationRepository;
import com.wpw.pim.repository.product.ProductRepository;
import com.wpw.pim.web.dto.product.ProductFilter;
import com.wpw.pim.web.dto.product.ProductSummaryDto;
import com.wpw.pim.service.product.ProductService;
import com.wpw.pim.web.dto.common.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
}
