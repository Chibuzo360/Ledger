package com.chinasaventures.ledger.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponseDTO(
        Long id,
        String customerName,
        String customerPhone,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        BigDecimal amountPaid,
        String paymentStatus,
        String paymentType,
        String paymentProof,
        UserSummaryDTO recordedBy,
        UserSummaryDTO confirmedBy,
        RetailerSummaryDTO retailer,
        LocalDateTime confirmedAt,
        LocalDateTime createdAt,

        // NEW -- appended at the very end, deliberately, not inserted
        // between existing fields. This is the same record type that had a
        // silent constructor-argument swap earlier this session; appending
        // at the tail means every EXISTING field keeps its exact position,
        // so this change cannot shift or corrupt anything already working.
        String paymentMethod,
        String paymentMethodNote
) {}