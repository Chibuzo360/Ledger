package com.chinasaventures.ledger.service;

import com.chinasaventures.ledger.model.Product;
import com.chinasaventures.ledger.model.ProductVariants;
import com.chinasaventures.ledger.model.Users;
import com.chinasaventures.ledger.repository.ProductRepository;
import com.chinasaventures.ledger.repository.ProductVariantsRepository;
import com.chinasaventures.ledger.dto.ProductVariantSummaryDTO;
import com.chinasaventures.ledger.repository.UsersRepository;
import com.chinasaventures.ledger.dto.ProductCategoryDTO;
import com.chinasaventures.ledger.dto.ProductSummaryDTO;
import com.chinasaventures.ledger.model.ProductCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductVariantsService {
    private final ProductVariantsRepository productVariantsRepository;
    private final UsersRepository usersRepository;
    private final ProductRepository productRepository; // NEW: to resolve the real parent product

    private void requireDirector(String message) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String identifier = auth.getName();
        Users currentUser = usersRepository.findByEmailOrPhoneNumber(identifier, identifier)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found: " + identifier));
        if (!"director".equals(currentUser.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
        }
    }

    public ProductCategory safeCategoryReturner (ProductVariants P){
        ProductCategory productCategory;
        if (P.getProduct() != null && P.getProduct().getCategory() !=null ) {
            productCategory = P.getProduct().getCategory();
        } else{
            productCategory = null;
        }
        return productCategory;
    }

    public ProductVariantSummaryDTO toDTO(ProductVariants pV){
        ProductCategoryDTO productCategory = safeCategoryReturner(pV) != null
                ? new ProductCategoryDTO(
                safeCategoryReturner(pV).getId(),
                safeCategoryReturner(pV).getName())
                : null;
        ProductSummaryDTO product = pV.getProduct() != null
                ? new ProductSummaryDTO(
                pV.getProduct().getId(),
                pV.getProduct().getName(),
                pV.getProduct().getPricePerUnit(),
                productCategory,
                pV.getProduct().getCurrentStock()
        )
                : null;
        return new ProductVariantSummaryDTO(
                pV.getId(),product,pV.getPricePerUnit(),pV.getSize(),pV.getProducer(),pV.getCurrentStock()
        );
    }

    public List<ProductVariantSummaryDTO> getAllProductVariants() {

        return productVariantsRepository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public ProductVariants getProductVariantsById(Long id){
        return productVariantsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: "+ id));
    }

    public ProductVariantSummaryDTO getProductVariantsDTOById(Long id){return toDTO(getProductVariantsById(id));}

    // CHANGED: director-only (previously ungated), consistent with
    // createProduct. Validates size/price/stock, and resolves the REAL
    // parent product from the database so the returned DTO carries the
    // product's name and category instead of an empty shell.
    public ProductVariantSummaryDTO createProductVariant(ProductVariants productVariant){
        requireDirector("Only a director can create a ProductVariant.");

        if (productVariant.getProduct() == null || productVariant.getProduct().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A variant must belong to a product.");
        }
        Product parent = productRepository.findById(productVariant.getProduct().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent product not found."));

        if (productVariant.getSize() == null || productVariant.getSize().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Variant size is required.");
        }
        if (productVariant.getPricePerUnit() == null
                || productVariant.getPricePerUnit().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Variant price is required and cannot be negative.");
        }
        Integer stock = productVariant.getCurrentStock();
        if (stock == null) {
            productVariant.setCurrentStock(0);
        } else if (stock < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock cannot be negative.");
        }

        productVariant.setProduct(parent);
        productVariant.setSize(productVariant.getSize().trim());
        return toDTO(productVariantsRepository.save(productVariant));
    }

    public List<ProductVariantSummaryDTO> getVariantsByProductId(Long productId) {
        return productVariantsRepository.findByProductId(productId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    // Deliberately NOT role-gated (staff can change a price), same as
    // ProductService.updateProduct.
    // CHANGED: no longer copies `product` or `currentStock` from the
    // request. Copying `product` let a variant be moved to another product
    // (or orphaned, if the body omitted it); copying `currentStock` let a
    // worker change stock with no reason and no audit row.
    public ProductVariantSummaryDTO updateProductVariant(Long id, ProductVariants updatedProductVariant){
        ProductVariants existing = getProductVariantsById(id);

        if (updatedProductVariant.getSize() == null || updatedProductVariant.getSize().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Variant size is required.");
        }
        if (updatedProductVariant.getPricePerUnit() == null
                || updatedProductVariant.getPricePerUnit().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Variant price is required and cannot be negative.");
        }

        existing.setPricePerUnit(updatedProductVariant.getPricePerUnit());
        existing.setSize(updatedProductVariant.getSize().trim());
        existing.setProducer(updatedProductVariant.getProducer());
        ProductVariants upToDateProductVariant = productVariantsRepository.save(existing);
        return toDTO(upToDateProductVariant);
    }

    // CHANGED: a variant that appears in sales/StockIn history now gives a
    // clear 409 instead of a raw foreign-key 500. (The 403 message
    // wording is left as-is on purpose.)
    public void deleteProductVariant(Long id){
        requireDirector("Only a director can delete a ProductVariant");
        try {
            productVariantsRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This variant appears in past sales, stock-received or stock-adjustment records and cannot be deleted.");
        }
    }
}