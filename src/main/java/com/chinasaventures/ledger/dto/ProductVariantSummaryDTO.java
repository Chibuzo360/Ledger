package com.chinasaventures.ledger.dto;

public record ProductVariantSummaryDTO(
        Long id,
        ProductSummaryDTO product,
        String size,
        String producer,
        int currentStock
) {
}
