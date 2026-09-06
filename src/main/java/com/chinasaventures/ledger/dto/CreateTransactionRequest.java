package com.chinasaventures.ledger.dto;

import java.math.BigDecimal;
import java.util.List;

// Deliberately has NO totalAmount field — that gets computed server-side
// from the items list, never trusted from the client.
public record CreateTransactionRequest(
        String customerName,
        String customerPhone,
        BigDecimal amountPaid,
        Long retailerId, // null for a walk-in customer
        List<TransactionItemRequest> items
) {}