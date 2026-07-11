package com.chinasaventures.ledger.dto;

public record UserSummaryDTO(Long id, String name, String role) {
    // CHANGED: record = compact immutable DTO, no Lombok needed
    // Only exposes what the frontend needs — no password, no email/phone
}