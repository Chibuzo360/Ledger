package com.chinasaventures.ledger.dto;

// Safe view of a Users row: deliberately has NO password/hash field, so the
// BCrypt hash can never be serialized to a client by accident.
// Save to: com.chinasaventures.ledger.dto
public record UserResponseDTO(
        Long id,
        String name,
        String email,
        String phoneNumber,
        String role,
        BranchSummaryDTO branch
) {}