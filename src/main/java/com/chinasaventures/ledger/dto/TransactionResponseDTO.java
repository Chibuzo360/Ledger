// New file: com.chinasaventures.ledger.dto.TransactionResponseDTO
package com.chinasaventures.ledger.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponseDTO(
        Long id,
        String customerName,
        String customerPhone,
        BigDecimal totalAmount,
        BigDecimal amountPaid,
        String paymentStatus,
        String paymentType,
        String paymentProof,
        UserSummaryDTO recordedBy,
        UserSummaryDTO confirmedBy,
        LocalDateTime confirmedAt,
        LocalDateTime createdAt
) {}