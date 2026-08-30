package com.chinasaventures.ledger.service;

import com.chinasaventures.ledger.model.Product;
import com.chinasaventures.ledger.model.Users;
import com.chinasaventures.ledger.repository.ProductRepository;
import com.chinasaventures.ledger.repository.UsersRepository;
import com.chinasaventures.ledger.repository.ProductVariantsRepository;
import com.chinasaventures.ledger.dto.ProductSummaryDTO;
import com.chinasaventures.ledger.dto.ProductCategoryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final UsersRepository usersRepository;
    private final ProductVariantsRepository productVariantsRepository;

    public ProductSummaryDTO toDTO (Product p){
        ProductCategoryDTO productCategory = p.getCategory() != null
                ? new ProductCategoryDTO(p.getCategory().getId(),p.getCategory().getName())
                : null ;
        return new ProductSummaryDTO(
                p.getId(), p.getName(),p.getPricePerUnit(),productCategory,
                p.getCurrentStock()
        );
    }

    public List<ProductSummaryDTO> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public Product getProductById(Long id){
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: "+ id));
    }

    public ProductSummaryDTO getProductDTOById(Long id){return toDTO(getProductById(id));}

    public ProductSummaryDTO createProduct(Product product){
        // will consider adding a "recordedBy" column to its entity
        Product savedProduct = productRepository.save(product);
        return  toDTO(savedProduct);
    }

    public ProductSummaryDTO updateProduct(Long id, Product updatedProduct){
        Product existing = getProductById(id);
        existing.setName(updatedProduct.getName());
        existing.setPricePerUnit(updatedProduct.getPricePerUnit());
        existing.setCategory(updatedProduct.getCategory());
        existing.setUnit(updatedProduct.getUnit());
        existing.setCurrentStock(updatedProduct.getCurrentStock());
        Product upToDateProduct = productRepository.save(existing);
        return toDTO(upToDateProduct);
    }

    public void deleteProduct(Long id){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String identifier = auth.getName();

        Users currentUser = usersRepository.findByEmailOrPhoneNumber(identifier, identifier)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found: " + identifier));
        //  look up the Users via usersRepository.findByEmailOrPhoneNumber(...)
        // if currentUser's role isn't "director", throw
        //         ResponseStatusException(HttpStatus.FORBIDDEN, "...")

        if(!"director".equals(currentUser.getRole())){
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Only a director can delete a Product");
        }

        boolean isPresent = productVariantsRepository.existsByProductId(id);
        if(!isPresent) {
            productRepository.deleteById(id);
        }else{
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.CONFLICT,"You are not allowed to delete a product with active Variants."
            );
        }
    }
}
