package com.chinasaventures.ledger.dto;

import java.math.BigDecimal;

public record TransactionSummaryDTO(
        Long id,
        String customerName,
        BigDecimal totalAmount,
        BigDecimal amountPaid,
        RetailerSummaryDTO retailer
) {// Only exposes what the frontend needs — no recorderdBy, no email/phone
}