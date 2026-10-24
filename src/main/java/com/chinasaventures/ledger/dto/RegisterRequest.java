package com.chinasaventures.ledger.dto;

public record RegisterRequestDTO(
        String name,
        String email,
        String phoneNumber,
        String password,
        String code) {} // NEW