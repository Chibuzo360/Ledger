package com.chinasaventures.ledger.service;

import com.chinasaventures.ledger.dto.*;
import com.chinasaventures.ledger.model.Product;
import com.chinasaventures.ledger.model.ProductCategory;
import com.chinasaventures.ledger.model.StockIn;
import com.chinasaventures.ledger.model.Users;
import com.chinasaventures.ledger.repository.ProductVariantsRepository;
import com.chinasaventures.ledger.repository.StockInRepository;
import com.chinasaventures.ledger.repository.ProductRepository;
import com.chinasaventures.ledger.repository.UsersRepository;
import com.chinasaventures.ledger.model.ProductVariants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockInService {
    private final StockInRepository stockInRepository;
    private final ProductRepository productRepository;
    private final ProductVariantsRepository productVariantsRepository;
    private final UsersRepository usersRepository;

    // NEW: same null-safe category pattern used in TransactionItemService --
    // avoids a NullPointerException walking product -> category when either
    // hop could be absent.
    private ProductCategory safeCategory(Product p) {
        return (p != null) ? p.getCategory() : null;
    }

    // NEW: the DTO conversion StockIn never had. Without this, every
    // lazy @ManyToOne relation below serializes to the frontend as a
    // silent null the instant the Hibernate session closes -- the exact
    // bug already fixed on every other entity in this codebase.
    private StockInResponseDTO toDTO(StockIn s) {
        Product product = s.getProduct();
        ProductVariants variant = s.getProductVariant();

        ProductCategoryDTO productCategory = safeCategory(product) != null
                ? new ProductCategoryDTO(safeCategory(product).getId(), safeCategory(product).getName())
                : null;

        ProductSummaryDTO productDTO = product != null
                ? new ProductSummaryDTO(
                product.getId(), product.getName(), product.getPricePerUnit(),
                productCategory, product.getCurrentStock())
                : null;

        ProductVariantSummaryDTO variantDTO = variant != null
                ? new ProductVariantSummaryDTO(
                variant.getId(), productDTO, variant.getPricePerUnit(),
                variant.getSize(), variant.getProducer(), variant.getCurrentStock())
                : null;

        UserSummaryDTO recordedByDTO = s.getRecordedBy() != null
                ? new UserSummaryDTO(s.getRecordedBy().getId(), s.getRecordedBy().getName(), s.getRecordedBy().getRole())
                : null;

        return new StockInResponseDTO(
                s.getId(), productDTO, variantDTO,
                s.getBranch() != null ? s.getBranch().getName() : null,
                s.getQuantity(), recordedByDTO,
                s.getTruckNumber(), s.getDeliveryNoteNumber(), s.getSupplierName(), s.getNote(),
                s.getCreatedAt()
        );
    }

    public List<StockInResponseDTO> getAllStockIn() {
        return stockInRepository.findAll().stream().map(this::toDTO).toList();
    }

    public StockIn getStockInById(Long id){
        return stockInRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("StockIn not found with id: "+ id));
    }

    public StockInResponseDTO getStockInDTOById(Long id) {
        return toDTO(getStockInById(id));
    }

    // CHANGED: this method previously trusted branch AND recordedBy
    // straight from the client-supplied request body -- a buggy or
    // malicious frontend could set recordedBy to any user's id, or record
    // stock into a branch the caller has nothing to do with. Both are now
    // derived from the JWT via SecurityContextHolder instead, matching
    // this codebase's established rule everywhere else (Transactions,
    // Expenses, StockAdjustment). @Transactional added because this is
    // now two writes (the StockIn row, then the stock increment) that
    // must succeed or fail together. Quantity and exactly-one-of-
    // product/variant are validated up front -- neither was checked at
    // all before.
    @Transactional
    public StockInResponseDTO addStockIn(StockIn stockIn) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String identifier = auth.getName();
        Users currentUser = usersRepository.findByEmailOrPhoneNumber(identifier, identifier)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found: " + identifier));

        if (stockIn.getQuantity() == null || stockIn.getQuantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Quantity must be greater than zero.");
        }

        boolean hasProduct = stockIn.getProduct() != null && stockIn.getProduct().getId() != null;
        boolean hasVariant = stockIn.getProductVariant() != null && stockIn.getProductVariant().getId() != null;

        if (hasProduct == hasVariant) {
            // both true (ambiguous) or both false (nothing to receive) are
            // equally invalid -- exactly one must be set.
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Provide either a product or a product variant, not both or neither.");
        }

        StockIn toSave = new StockIn();
        toSave.setQuantity(stockIn.getQuantity());
        toSave.setTruckNumber(stockIn.getTruckNumber());
        toSave.setDeliveryNoteNumber(stockIn.getDeliveryNoteNumber());
        toSave.setSupplierName(stockIn.getSupplierName());
        toSave.setNote(stockIn.getNote());
        toSave.setRecordedBy(currentUser); // NEW: derived, never trusted from client
        toSave.setBranch(currentUser.getBranch()); // NEW: derived, never trusted from client

        if (hasVariant) {
            ProductVariants variant = productVariantsRepository.findById(stockIn.getProductVariant().getId())
                    .orElseThrow(() -> new RuntimeException(
                            "Variant not found with id: " + stockIn.getProductVariant().getId()));
            toSave.setProductVariant(variant);
            variant.setCurrentStock(variant.getCurrentStock() + toSave.getQuantity());
            productVariantsRepository.save(variant);
        } else {
            Product product = productRepository.findById(stockIn.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException(
                            "Product not found with id: " + stockIn.getProduct().getId()));
            toSave.setProduct(product);
            product.setCurrentStock(product.getCurrentStock() + toSave.getQuantity());
            productRepository.save(product);
        }

        StockIn saved = stockInRepository.save(toSave);
        return toDTO(saved);
    }

    // CHANGED: previously just deleted the row with no stock reversal at
    // all -- correcting a mistaken StockIn by deleting it silently left
    // the stock increase in place forever, breaking the whole point of
    // "delete and re-record" as a correction mechanism. Now reverses
    // EXACTLY the quantity this record added, from whichever side
    // (product or variant) it actually went to, before removing the row.
    // Director-only, matching every other delete in this codebase that
    // has real inventory/financial consequences (Product, ProductVariants,
    // Retailers). @Transactional since this is now two writes.
    @Transactional
    public void deleteStockIn(Long id){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String identifier = auth.getName();
        Users currentUser = usersRepository.findByEmailOrPhoneNumber(identifier, identifier)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found: " + identifier));

        if (!"director".equals(currentUser.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only a director can delete a stock-in record.");
        }

        StockIn stockIn = getStockInById(id);

        if (stockIn.getProductVariant() != null) {
            ProductVariants variant = stockIn.getProductVariant();
            variant.setCurrentStock(variant.getCurrentStock() - stockIn.getQuantity());
            productVariantsRepository.save(variant);
        } else if (stockIn.getProduct() != null) {
            Product product = stockIn.getProduct();
            product.setCurrentStock(product.getCurrentStock() - stockIn.getQuantity());
            productRepository.save(product);
        }

        stockInRepository.deleteById(id);
    }

    // NEW: full receiving history for one product, newest first -- same
    // pattern as StockAdjustmentService's history endpoints.
    public List<StockInResponseDTO> getHistoryForProduct(Long productId) {
        return stockInRepository.findByProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    // NEW: same, for one variant.
    public List<StockInResponseDTO> getHistoryForVariant(Long variantId) {
        return stockInRepository.findByProductVariantIdOrderByCreatedAtDesc(variantId)
                .stream()
                .map(this::toDTO)
                .toList();
    }
}