package com.chinasaventures.ledger.dto;

public record ProductSummaryDTO (
        Long id,
        String name,
        ProductCategoryDTO productCategory,
        int currentStock
){}
