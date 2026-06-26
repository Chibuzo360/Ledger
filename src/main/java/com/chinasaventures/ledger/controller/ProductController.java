package com.chinasaventures.ledger.controller;

import com.chinasaventures.ledger.model.Product;
import com.chinasaventures.ledger.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor

public class ProductController {


        private final ProductService productService;

        @GetMapping
        public ResponseEntity<List<Product>> getAllProducts() {
        System.out.println("GET /api/products reached");
        return ResponseEntity.ok(productService.getAllProducts());
        }

        @GetMapping("/{id}")
        public ResponseEntity<Product> getProductById(@PathVariable Long id) {
            return ResponseEntity.ok(productService.getProductById(id));
        }

        @PostMapping
        public ResponseEntity<Product> createProduct(@RequestBody Product product) {
            return ResponseEntity.ok(productService.createProduct(product));
        }

        @PutMapping("/{id}")
        public ResponseEntity<Product> updateProduct(@PathVariable Long id,
                                                     @RequestBody Product product) {
            return ResponseEntity.ok(productService.updateProduct(id, product));
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
            productService.deleteProduct(id);
            return ResponseEntity.noContent().build();
        }
}
