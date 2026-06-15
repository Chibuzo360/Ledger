package com.chinasaventures.ledger.controller;

import com.chinasaventures.ledger.model.Retailers;
import com.chinasaventures.ledger.service.RetailersService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class RetailersController {

    private RetailersService retailersService;

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
    public ResponseEntity<Retailers> deleteRetailer(@PathVariable Long id){
        retailersService.deleteRetailer(id);
        return ResponseEntity.noContent().build();
    }
}
