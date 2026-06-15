package com.chinasaventures.ledger.controller;

import com.chinasaventures.ledger.model.Retailers;
import com.chinasaventures.ledger.service.RetailersService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/retailers")
@RequiredArgsConstructor
public class RetailersController {

    private final RetailersService retailersService;

    @GetMapping
    public ResponseEntity<List<Retailers>> getAllRetailers(){
        return ResponseEntity.ok(retailersService.getAllRetailers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Retailers> getRetailerById(@PathVariable Long id) {
        return ResponseEntity.ok(retailersService.getRetailerById(id));
    }

    @PostMapping
    public ResponseEntity<Retailers> createRetailer(@RequestBody Retailers retailer){
        return ResponseEntity.ok(retailersService.createRetailer(retailer));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Retailers> updateRetailers(@PathVariable Long id, @RequestBody Retailers retailer){
        return ResponseEntity.ok(retailersService.updateRetailers(id,retailer));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRetailer(@PathVariable Long id){
        retailersService.deleteRetailer(id);
        return ResponseEntity.noContent().build();
    }
}
