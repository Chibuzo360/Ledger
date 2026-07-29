package com.chinasaventures.ledger.dto;

public record RetailerSummaryDTO(Long id, String businessName, String contactName) {
    // just enough for the frontend to display and link without a separate lookup
}