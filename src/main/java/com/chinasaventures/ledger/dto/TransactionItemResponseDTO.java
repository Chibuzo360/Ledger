package com.chinasaventures.ledger.dto;

public record TransactionItemResponseDTO(
        Long id,
        TransactionSummaryDTO transaction,
        ProductSummaryDTO product, //added newly
        ProductVariantSummaryDTO productVariant,
        int quantityOrdered,
        int quantitySupplied,
        String supplyStatus
) {}
