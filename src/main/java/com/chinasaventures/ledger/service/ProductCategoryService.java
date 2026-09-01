package com.chinasaventures.ledger.service;

import com.chinasaventures.ledger.model.Product;
import com.chinasaventures.ledger.model.ProductCategory;
import com.chinasaventures.ledger.model.Users;
import com.chinasaventures.ledger.repository.ProductCategoryRepository;
import com.chinasaventures.ledger.repository.ProductRepository;
import com.chinasaventures.ledger.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductCategoryService {

    private final ProductCategoryRepository productCategoryRepository;
    private final UsersRepository usersRepository;
    private final ProductRepository productRepository;

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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String identifier = auth.getName();

        Users currentUser = usersRepository.findByEmailOrPhoneNumber(identifier,identifier)
                        .orElseThrow(() -> new RuntimeException("Logged-in user not found: " + identifier));

        if(!"director".equals(currentUser.getRole())){
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Only a director can delete a Product Category.");
        }
        boolean hasProduct = productRepository.existsByCategoryId(id);
        if(!hasProduct){
            productCategoryRepository.deleteById(id);
        }else{
           List <Product> productWithCategory = productRepository.findByCategoryId(id);
           //I should loop through the list above and set each of their category to null, then saveAll.
        }


    }
}