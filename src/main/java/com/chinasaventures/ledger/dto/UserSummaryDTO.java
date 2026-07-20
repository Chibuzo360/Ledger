package com.chinasaventures.ledger.dto;

public record UserSummaryDTO(Long id, String name, String role) {
    // Only exposes what the frontend needs — no password, no email/phone
}