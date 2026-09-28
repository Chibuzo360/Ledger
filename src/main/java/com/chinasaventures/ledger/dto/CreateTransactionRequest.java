package com.chinasaventures.ledger.dto;

import java.math.BigDecimal;
import java.util.List;

// Deliberately has NO totalAmount field -- that gets computed server-side
// from the items list, never trusted from the client.
public record CreateTransactionRequest(
        String customerName,
        String customerPhone,
        BigDecimal amountPaid,
        BigDecimal discountAmount, //Treated as 0 by default
        Long retailerId, // null for a walk-in customer
        List<TransactionItemRequest> items,

        // NEW: CASH / POS_TRANSFER / POS_CARD / BUSINESS_ACCOUNT / MIXED --
        // required, validated in TransactionsService.addTransaction().
        String paymentMethod,

        // NEW: required when paymentMethod is MIXED, ignored otherwise.
        String paymentMethodNote,

        // NEW: only meaningful when paymentMethod is a witnessed method
        // (CASH/POS_TRANSFER/POS_CARD) -- the clerk directly saw the money
        // land and is marking it confirmed right away. Ignored for
        // BUSINESS_ACCOUNT/MIXED, which always start pending regardless of
        // what this flag says (only a director can verify those). Safe to
        // omit from the JSON entirely -- Jackson maps a missing field to
        // null, treated the same as false.
        Boolean confirmedAtCreation
) {}