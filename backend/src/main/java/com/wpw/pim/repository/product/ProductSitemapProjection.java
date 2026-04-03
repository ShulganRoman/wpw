package com.wpw.pim.repository.product;

import java.time.OffsetDateTime;

public interface ProductSitemapProjection {
    String getToolNo();
    OffsetDateTime getUpdatedAt();
}
