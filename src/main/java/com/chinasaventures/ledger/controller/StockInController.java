package com.chinasaventures.ledger.controller;


import com.chinasaventures.ledger.dto.StockInResponseDTO;
import com.chinasaventures.ledger.model.StockIn;
import com.chinasaventures.ledger.service.StockInService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock_in")
@RequiredArgsConstructor

public class StockInController {

    private final StockInService stockInService;

    // CHANGED: StockIn -> StockInResponseDTO, matching the DTO fix in the service.
    @GetMapping
    public ResponseEntity<List<StockInResponseDTO>> getAllStockIn(){
        return ResponseEntity.ok(stockInService.getAllStockIn());
    }

    // CHANGED: StockIn -> StockInResponseDTO, via the new DTO-returning getter.
    @GetMapping("/{id}")
    public ResponseEntity<StockInResponseDTO> getStockInById(@PathVariable Long id){
        return ResponseEntity.ok(stockInService.getStockInDTOById(id));
    }

    // CHANGED: response is now StockInResponseDTO. Request body stays StockIn --
    // that's the incoming shape from the frontend form; the service now
    // ignores whatever branch/recordedBy it contains and derives both from
    // the JWT instead, so sending them (or not) makes no difference.
    @PostMapping
    public ResponseEntity<StockInResponseDTO> addStockIn(@RequestBody StockIn stockIn){
        return ResponseEntity.ok(stockInService.addStockIn(stockIn));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStockIn(@PathVariable Long id){
        stockInService.deleteStockIn(id);
        return  ResponseEntity.noContent().build();
    }

    // NEW: stock-in history for one product/variant -- powers the
    // "view details" action on ProductPage.
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<StockInResponseDTO>> getHistoryForProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(stockInService.getHistoryForProduct(productId));
    }

    @GetMapping("/variant/{variantId}")
    public ResponseEntity<List<StockInResponseDTO>> getHistoryForVariant(@PathVariable Long variantId) {
        return ResponseEntity.ok(stockInService.getHistoryForVariant(variantId));
    }
} 