package com.chinasaventures.ledger.controller;

import com.chinasaventures.ledger.dto.DiscountSettingsDTO;
import com.chinasaventures.ledger.service.DiscountSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/discount-settings")
@RequiredArgsConstructor
public class DiscountSettingsController {
    private final DiscountSettingsService discountSettingsService;

    @GetMapping
    public ResponseEntity<DiscountSettingsDTO> getSettings() {
        return ResponseEntity.ok(discountSettingsService.getSettingsDTO());
    }

    public record UpdateMaxDiscountRequest(BigDecimal maxDiscountAmount) {}

    @PutMapping
    public ResponseEntity<DiscountSettingsDTO> updateSettings(@RequestBody UpdateMaxDiscountRequest request) {
        return ResponseEntity.ok(discountSettingsService.updateMaxDiscount(request.maxDiscountAmount()));
    }

    // NEW
    public record UpdateWorkerConfirmationRequest(Boolean allowWorkerConfirmation) {}

    // NEW: separate endpoint rather than folding into the existing PUT --
    // keeps the discount cap and the confirmation toggle as two
    // independent updates, so changing one can never accidentally touch
    // the other.
    @PutMapping("/worker-confirmation")
    public ResponseEntity<DiscountSettingsDTO> updateWorkerConfirmation(
            @RequestBody UpdateWorkerConfirmationRequest request) {
        return ResponseEntity.ok(
                discountSettingsService.updateAllowWorkerConfirmation(request.allowWorkerConfirmation()));
    }
}