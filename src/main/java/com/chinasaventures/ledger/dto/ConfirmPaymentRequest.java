package com.chinasaventures.ledger.dto;

import java.math.BigDecimal;

// NEW: request body shape for updating/confirming a payment.
// amountPaid is optional — null means "just confirm, don't change the amount"
public record ConfirmPaymentRequest(BigDecimal amountPaid, String paymentProof) {}