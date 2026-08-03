package com.chinasaventures.ledger.dto;

import java.math.BigDecimal;

public record ProductVariantSummaryDTO(
        Long id,
        ProductSummaryDTO product,
        BigDecimal pricePerUnit,
        String size,
        String producer,
        int currentStock
) {
}
