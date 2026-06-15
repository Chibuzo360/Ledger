package com.chinasaventures.ledger.controller;


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

    @GetMapping
    public ResponseEntity<List<StockIn>> getAllStockIn(){
        return ResponseEntity.ok(stockInService.getAllStockIn());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StockIn> getStockInById(@PathVariable Long id){
        return ResponseEntity.ok(stockInService.getStockInById(id));
    }

    @PostMapping
    public ResponseEntity<StockIn> addStockIn(@RequestBody StockIn stockIn){
        return ResponseEntity.ok(stockInService.addStockIn(stockIn));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStockIn(@PathVariable Long id){
        stockInService.deleteStockIn(id);
        return  ResponseEntity.noContent().build();
    }
}
