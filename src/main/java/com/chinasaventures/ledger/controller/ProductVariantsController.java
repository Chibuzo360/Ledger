package com.chinasaventures.ledger.controller;

import com.chinasaventures.ledger.model.ProductVariants;
import com.chinasaventures.ledger.service.ProductVariantsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product-variants")
@RequiredArgsConstructor
public class ProductVariantsController {

    private final ProductVariantsService productVariantsService;

    @GetMapping
    public ResponseEntity<List<ProductVariants>> getAllProductVariants() {
        return ResponseEntity.ok(productVariantsService.getAllProductVariants());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductVariants> getProductVariantById(@PathVariable Long id) {
        return ResponseEntity.ok(productVariantsService.getProductVariantsById(id));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ProductVariants>> getVariantsByProductId(@PathVariable Long productId) {
        return ResponseEntity.ok(productVariantsService.getVariantsByProductId(productId));
    }

    @PostMapping
    public ResponseEntity<ProductVariants> createProductVariant(@RequestBody ProductVariants productVariant) {
        return ResponseEntity.ok(productVariantsService.createProductVariant(productVariant));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductVariants> updateProductVariant(@PathVariable Long id,
                                                                @RequestBody ProductVariants productVariant) {
        return ResponseEntity.ok(productVariantsService.updateProductVariant(id, productVariant));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProductVariant(@PathVariable Long id) {
        productVariantsService.deleteProductVariant(id);
        return ResponseEntity.noContent().build();
    }
}