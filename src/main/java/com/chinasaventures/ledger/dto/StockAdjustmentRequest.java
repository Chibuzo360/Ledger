package com.chinasaventures.ledger.dto;

// NEW — request body for POST /api/stock-adjustments.
// productVariantId is optional (null for variant-less products), matching
// the same optional-variant pattern used in TransactionItemRequest.
public record StockAdjustmentRequest(
        Long productId,
        Long productVariantId,
        Integer newStock,
        String reason
) {}