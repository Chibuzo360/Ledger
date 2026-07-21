package com.chinasaventures.ledger.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExpensesResponseDTO(
        Long id,
        String description,
        BigDecimal amount,
        UserSummaryDTO recordedBy,
        BranchSummaryDTO branch,
        LocalDateTime createdAt
){}
