package com.chinasaventures.ledger.service;


import com.chinasaventures.ledger.model.ProductVariants;
import com.chinasaventures.ledger.repository.ProductVariantsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductVariantsService {
    private final ProductVariantsRepository productVariantsRepository;

    public List<ProductVariants> getAllProductVariants() {
        return productVariantsRepository.findAll();
    }

    public ProductVariants getProductVariantsById(Long id){
        return productVariantsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: "+ id));
    }

    public ProductVariants createProductVariant(ProductVariants productVariant){
        return productVariantsRepository.save(productVariant);
    }

    public List<ProductVariants> getVariantsByProductId(Long productId) {
        return productVariantsRepository.findByProductId(productId);
    }

    public ProductVariants updateProductVariant(Long id, ProductVariants updatedProductVariant){
        ProductVariants existing = getProductVariantsById(id);
        existing.setProduct(updatedProductVariant.getProduct());
        existing.setPricePerUnit(updatedProductVariant.getPricePerUnit());
        existing.setCurrentStock(updatedProductVariant.getCurrentStock());
        existing.setSize(updatedProductVariant.getSize());
        existing.setProducer(updatedProductVariant.getProducer());
        return productVariantsRepository.save(existing);
    }

    public void deleteProductVariant(Long id){
        productVariantsRepository.deleteById(id);
    }
}
