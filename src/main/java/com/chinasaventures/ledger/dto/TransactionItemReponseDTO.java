package com.chinasaventures.ledger.dto;

import com.chinasaventures.ledger.model.ProductVariants;

public record TransactionItemReponseDTO(
        Long id,
        TransactionSummaryDTO transaction,
        ProductSummaryDTO product,
        ProductVariantSummmaryDTO productVariant,
        int quantityOrdered,
        int quantitySupplied,
        String supplyStatus
) {}
