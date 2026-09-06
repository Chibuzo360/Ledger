package com.chinasaventures.ledger.dto;

// One line in the cart. Field names must match exactly what the frontend
// sends as JSON keys — same class of bug as the productCategory/category
// mismatch earlier this session, so double-check this against whatever
// the Record Sale form actually posts once it's built.
public record TransactionItemRequest(
        Long productId,
        Long productVariantId, // null if this line is a variant-less product
        Integer quantityOrdered,
        Integer quantitySupplied,
        String supplyNote // null/omit if not applicable
) {}