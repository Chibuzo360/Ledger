package com.chinasaventures.ledger.dto;

import java.math.BigDecimal;

public record ProductSummaryDTO (
        Long id,
        String name,
        BigDecimal pricePerUnit,
        ProductCategoryDTO productCategory,
        int currentStock
){}
