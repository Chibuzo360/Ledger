package com.chinasaventures.ledger.dto;

// NEW — request body for PUT /api/transaction_item/{id}/supply.
// additionalQuantity is how many MORE units are leaving today — not the
// new total. The endpoint adds this to the item's existing quantitySupplied.
public record SupplyRemainingRequest(
        Integer additionalQuantity,
        String note
) {}