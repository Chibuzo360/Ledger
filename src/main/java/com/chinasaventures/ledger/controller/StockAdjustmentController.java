package com.chinasaventures.ledger.controller;

import com.chinasaventures.ledger.dto.StockAdjustmentRequest;
import com.chinasaventures.ledger.dto.StockAdjustmentResponseDTO;
import com.chinasaventures.ledger.service.StockAdjustmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// NEW — no PUT/DELETE at all, on purpose. Same immutable-log philosophy as
// StockIn: a correction to a correction is a new POST, never an edit to an
// existing row.
@RestController
@RequestMapping("/api/stock-adjustments")
@RequiredArgsConstructor
public class StockAdjustmentController {

    private final StockAdjustmentService stockAdjustmentService;

    @PostMapping
    public ResponseEntity<StockAdjustmentResponseDTO> adjustStock(@RequestBody StockAdjustmentRequest request) {
        return ResponseEntity.ok(stockAdjustmentService.adjustStock(request));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<StockAdjustmentResponseDTO>> getHistoryForProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(stockAdjustmentService.getHistoryForProduct(productId));
    }

    @GetMapping("/variant/{variantId}")
    public ResponseEntity<List<StockAdjustmentResponseDTO>> getHistoryForVariant(@PathVariable Long variantId) {
        return ResponseEntity.ok(stockAdjustmentService.getHistoryForVariant(variantId));
    }
}