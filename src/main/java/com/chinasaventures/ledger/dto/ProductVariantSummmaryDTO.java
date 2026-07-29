package com.chinasaventures.ledger.dto;

public record ProductVariantSummmaryDTO(
        Long id,
        ProductSummaryDTO product,
        String size,
        String producer,
        int currentStock
) {
}
