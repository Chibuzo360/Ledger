package com.chinasaventures.ledger.service;

import com.chinasaventures.ledger.model.Product;
import com.chinasaventures.ledger.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product getProductById(Long id){
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: "+ id));
    }

    public Product createProduct(Product product){
        return productRepository.save(product);
    }

    public Product updateProduct(Long id, Product updatedProduct){
        Product existing = getProductById(id);
        existing.setName(updatedProduct.getName());
        existing.setUnit(updatedProduct.getUnit());
        existing.setCurrentStock(updatedProduct.getCurrentStock());
        return productRepository.save(existing);
    }

    public void deleteProduct(Long id){
        productRepository.deleteById(id);
    }
}
