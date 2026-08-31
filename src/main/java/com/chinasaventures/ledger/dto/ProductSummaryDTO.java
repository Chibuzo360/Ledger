package com.chinasaventures.ledger.dto;

import java.math.BigDecimal;

public record ProductSummaryDTO (
        Long id,
        String name,
        BigDecimal pricePerUnit,
        ProductCategoryDTO category,
        int currentStock
){}
