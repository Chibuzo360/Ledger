package com.chinasaventures.ledger.dto;

import java.time.LocalDateTime;

// NEW — deliberately flat (plain Strings/primitives), NOT nesting your
// existing ProductSummaryDTO/ProductVariantSummaryDTO/UserSummaryDTO
// records. I haven't seen those files this session, and those are
// position-sensitive records per your own handoff notes — guessing their
// field order wrong would silently shift data with no compile error. Flat
// fields here are slower to extend later but can't misfire silently.
// Swap to nested Summary DTOs yourself once you've confirmed their real
// constructors, if you'd rather have the richer shape.
public record StockAdjustmentResponseDTO(
        Long id,
        Long productId,
        String productName,
        Long productVariantId,
        String variantDescription, // e.g. "40x40 - Portobello", null if no variant
        Integer previousStock,
        Integer newStock,
        String reason,
        Long adjustedById,
        String adjustedByName,
        LocalDateTime createdAt
) {}