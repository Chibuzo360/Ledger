package com.chinasaventures.ledger.service;

import com.chinasaventures.ledger.model.Product;
import com.chinasaventures.ledger.model.Users;
import com.chinasaventures.ledger.repository.ProductRepository;
import com.chinasaventures.ledger.repository.UsersRepository;
import com.chinasaventures.ledger.repository.ProductVariantsRepository;
import com.chinasaventures.ledger.repository.StockAdjustmentRepository;
import com.chinasaventures.ledger.dto.ProductSummaryDTO;
import com.chinasaventures.ledger.dto.ProductCategoryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final UsersRepository usersRepository;
    private final ProductVariantsRepository productVariantsRepository;
    private final StockAdjustmentRepository stockAdjustmentRepository;

    private void requireDirector(String message) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String identifier = auth.getName();
        Users currentUser = usersRepository.findByEmailOrPhoneNumber(identifier, identifier)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found: " + identifier));
        if (!"director".equals(currentUser.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
        }
    }

    // NEW: name/unit are NOT NULL in the DB, so a blank one used to surface
    // as a raw constraint error. A null price is treated as 0 (Jackson
    // overwrites the entity's "= BigDecimal.ZERO" default with an explicit
    // null when the client sends one, and a NULL price would later NPE in
    // addTransaction()). Negative price is rejected.
    private void trimAndValidateName(Product p) {
        if (p.getName() == null || p.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product name is required.");
        }
        if (p.getUnit() == null || p.getUnit().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product unit is required.");
        }
        p.setName(p.getName().trim());
        p.setUnit(p.getUnit().trim());
    }

    private BigDecimal validatedPrice(BigDecimal price) {
        if (price == null) return BigDecimal.ZERO;
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Price cannot be negative.");
        }
        return price;
    }

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

    // Director-only: only the director dictates how new products are created.
    // CHANGED: the 403 message said "delete" (copy-paste slip); validation added.
    public ProductSummaryDTO createProduct(Product product){
        requireDirector("Only a director can create a Product.");

        trimAndValidateName(product);
        product.setPricePerUnit(validatedPrice(product.getPricePerUnit()));

        // Opening stock on creation is allowed (it is the initial value);
        // afterwards stock only moves via sales, StockIn and adjustments.
        Integer stock = product.getCurrentStock();
        if (stock == null) {
            product.setCurrentStock(0);
        } else if (stock < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock cannot be negative.");
        }

        Product savedProduct = productRepository.save(product);
        return toDTO(savedProduct);
    }

    // Deliberately NOT role-gated, so staff can change a price (ChangedBy
    // tracking is a v2 item).
    // CHANGED: no longer copies currentStock from the request. Before, a
    // worker could change stock through this ordinary edit call with no
    // reason and no StockAdjustment row, bypassing the director-only audit
    // log. Stock now changes only through sales, StockIn and adjustments.
    public ProductSummaryDTO updateProduct(Long id, Product updatedProduct){
        Product existing = getProductById(id);

        trimAndValidateName(updatedProduct);
        existing.setName(updatedProduct.getName());
        existing.setUnit(updatedProduct.getUnit());
        existing.setPricePerUnit(validatedPrice(updatedProduct.getPricePerUnit()));
        existing.setCategory(updatedProduct.getCategory());

        Product upToDateProduct = productRepository.save(existing);
        return toDTO(upToDateProduct);
    }

    // @Transactional: nullifying the StockAdjustment rows and deleting the
    // product are two writes that must succeed or fail together.
    // CHANGED: a product that still appears in sales or StockIn history
    // used to hit a raw Postgres FK error (500) AFTER this method returned
    // (at commit). deleteById is now flushed inside a try/catch so it
    // becomes a clear 409, and the rollback also undoes the nullify step,
    // so a failed delete never detaches the adjustment history.
    @Transactional
    public void deleteProduct(Long id){
        requireDirector("Only a director can delete a Product.");

        if (!productRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found with id: " + id);
        }

        boolean hasVariants = productVariantsRepository.existsByProductId(id);
        if (hasVariants) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "You are not allowed to delete a product with active Variants.");
        }

        stockAdjustmentRepository.nullifyProductReference(id);
        try {
            productRepository.deleteById(id);
            productRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This product appears in past sales or stock-received records and cannot be deleted.");
        }
    }
}