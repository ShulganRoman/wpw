package com.wpw.pim.web.dto.cart;

import java.util.List;

public record CartImportResult(
    int imported,
    int replaced,
    List<String> errors,
    CartDto cart
) {}
