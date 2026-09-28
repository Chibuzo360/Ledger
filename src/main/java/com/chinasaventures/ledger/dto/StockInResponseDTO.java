package com.chinasaventures.ledger.dto;

import java.time.LocalDateTime;

// NEW -- StockIn never had a response DTO, which meant its lazy @ManyToOne
// relations (productVariant, product, branch, recordedBy) were being
// serialized raw, straight into the same "lazy proxy -> silent null" bug
// already fixed everywhere else in this codebase. branch is deliberately
// flat (branchName only, not a nested BranchSummaryDTO) since no such DTO
// has been confirmed to exist yet -- same risk-avoidance reasoning used
// for StockAdjustmentResponseDTO earlier this session.
public record StockInResponseDTO(
        Long id,
        ProductSummaryDTO product,
        ProductVariantSummaryDTO productVariant,
        String branchName,
        Integer quantity,
        UserSummaryDTO recordedBy,
        String truckNumber,
        String deliveryNoteNumber,
        String supplierName,
        String note,
        LocalDateTime createdAt
) {}