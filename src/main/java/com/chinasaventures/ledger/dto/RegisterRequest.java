package com.chinasaventures.ledger.dto;

public record RegisterRequest(
        String name,
        String email,
        String phoneNumber,
        String password,
        String code) {} // NEW