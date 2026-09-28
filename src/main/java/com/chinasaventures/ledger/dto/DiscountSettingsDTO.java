package com.chinasaventures.ledger.dto;

import java.math.BigDecimal;

// Reconstructed from its one call site in DiscountSettingsService.toDTO()
// -- the original two-arg version (id, maxDiscountAmount) is unchanged,
// allowWorkerConfirmation is appended as a third field, same tail-append
// safety reasoning as TransactionResponseDTO above.
public record DiscountSettingsDTO(
        Long id,
        BigDecimal maxDiscountAmount,
        Boolean allowWorkerConfirmation
) {}