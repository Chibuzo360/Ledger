package com.chinasaventures.ledger.service;

import com.chinasaventures.ledger.model.Product;
import com.chinasaventures.ledger.model.ProductCategory;
import com.chinasaventures.ledger.model.Users;
import com.chinasaventures.ledger.repository.ProductCategoryRepository;
import com.chinasaventures.ledger.repository.ProductRepository;
import com.chinasaventures.ledger.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductCategoryService {

    private final ProductCategoryRepository productCategoryRepository;
    private final UsersRepository usersRepository;
    private final ProductRepository productRepository;

    private void requireDirector(String message) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String identifier = auth.getName();
        Users currentUser = usersRepository.findByEmailOrPhoneNumber(identifier, identifier)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found: " + identifier));
        if (!"director".equals(currentUser.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
        }
    }

    // Blank check + case-insensitive duplicate check. excludeId lets a
    // rename keep its own current name without tripping the duplicate rule.
    private String validatedName(String rawName, Long excludeId) {
        if (rawName == null || rawName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category name is required.");
        }
        String name = rawName.trim();
        boolean duplicate = productCategoryRepository.findAll().stream()
                .anyMatch(c -> c.getName() != null
                        && c.getName().equalsIgnoreCase(name)
                        && (excludeId == null || !c.getId().equals(excludeId)));
        if (duplicate) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A category named \"" + name + "\" already exists.");
        }
        return name;
    }

    public List<ProductCategory> getAllCategories() {
        return productCategoryRepository.findAll();
    }

    public ProductCategory getCategoryById(Long id) {
        return productCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
    }

    // CHANGED: director-only (previously ungated) + name validation.
    public ProductCategory createCategory(ProductCategory category) {
        requireDirector("Only a director can create a Product Category.");
        category.setName(validatedName(category.getName(), null));
        try {
            return productCategoryRepository.save(category);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That category name already exists.");
        }
    }

    // CHANGED: director-only (previously ungated) + name validation.
    public ProductCategory updateCategory(Long id, ProductCategory updatedCategory) {
        requireDirector("Only a director can rename a Product Category.");
        ProductCategory existing = getCategoryById(id);
        existing.setName(validatedName(updatedCategory.getName(), id));
        try {
            return productCategoryRepository.save(existing);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That category name already exists.");
        }
    }

    // CHANGED: @Transactional, because this is two writes (orphan the
    // products, then delete the category) that must succeed or fail
    // together. Also removed an unused AuthenticationManager import.
    @Transactional
    public void deleteProductCategory(Long id) {
        requireDirector("Only a director can delete a Product Category.");

        boolean hasProduct = productRepository.existsByCategoryId(id);
        if (hasProduct) {
            List<Product> productWithCategory = productRepository.findByCategoryId(id);
            productWithCategory.forEach(product -> product.setCategory(null));
            productRepository.saveAll(productWithCategory);
        }

        productCategoryRepository.deleteById(id);
    }
}