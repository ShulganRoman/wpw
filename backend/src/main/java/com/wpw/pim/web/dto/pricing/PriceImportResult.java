package com.wpw.pim.web.dto.pricing;

import java.util.List;

public record PriceImportResult(int imported, int skipped, List<String> errors) {}
