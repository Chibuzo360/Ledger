package com.chinasaventures.ledger.dto;

public record BranchSummaryDTO(Long id, String name) {
    // CHANGED: record = compact immutable DTO, no Lombok needed
    // Only exposes what the frontend needs — no password, no email/phone
}