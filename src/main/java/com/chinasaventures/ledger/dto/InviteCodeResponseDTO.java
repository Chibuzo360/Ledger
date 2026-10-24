package com.chinasaventures.ledger.dto;

import java.time.LocalDateTime;

public record InviteCodeResponseDTO(String code, String role, LocalDateTime expiresAt) {} // NEW