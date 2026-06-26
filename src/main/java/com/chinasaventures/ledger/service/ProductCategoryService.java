package com.chinasaventures.ledger.service;

import com.chinasaventures.ledger.model.ProductCategory;
import com.chinasaventures.ledger.repository.ProductCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductCategoryService {

    private final ProductCategoryRepository productCategoryRepository;

    public List<ProductCategory> getAllCategories() {
        return productCategoryRepository.findAll();
    }

    public ProductCategory getCategoryById(Long id) {
        return productCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
    }

    public ProductCategory createCategory(ProductCategory category) {
        return productCategoryRepository.save(category);
    }

    public ProductCategory updateCategory(Long id, ProductCategory updatedCategory) {
        ProductCategory existing = getCategoryById(id);
        existing.setName(updatedCategory.getName());
        return productCategoryRepository.save(existing);
    }

    public void deleteProductCategory(Long id) {
        productCategoryRepository.deleteById(id);
    }
}