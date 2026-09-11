package com.chinasaventures.ledger.service;

import com.chinasaventures.ledger.dto.StockAdjustmentRequest;
import com.chinasaventures.ledger.dto.StockAdjustmentResponseDTO;
import com.chinasaventures.ledger.model.*;
import com.chinasaventures.ledger.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

// NEW
@Service
@RequiredArgsConstructor
public class StockAdjustmentService {

    private final StockAdjustmentRepository stockAdjustmentRepository;
    private final ProductRepository productRepository;
    private final ProductVariantsRepository productVariantsRepository;
    private final UsersRepository usersRepository;

    // CHANGED: product can now be null (deleted, reference nullified) — the
    // DTO falls back to productNameSnapshot in that case instead of NPE-ing
    // on p.getName(). productId in the DTO is null when the product is gone,
    // which the frontend can use to know not to link to it anymore.
    private StockAdjustmentResponseDTO toDTO(StockAdjustment a) {
        Product p = a.getProduct();
        ProductVariants v = a.getProductVariant();

        String variantDescription = v != null
                ? v.getSize() + " - " + v.getProducer()
                : null;

        return new StockAdjustmentResponseDTO(
                a.getId(),
                p != null ? p.getId() : null,
                p != null ? p.getName() : a.getProductNameSnapshot(),
                v != null ? v.getId() : null,
                variantDescription,
                a.getPreviousStock(),
                a.getNewStock(),
                a.getReason(),
                a.getAdjustedBy().getId(),
                a.getAdjustedBy().getName(),
                a.getCreatedAt()
        );
    }

    // Director-only, same service-layer check pattern used everywhere else
    // in this codebase (never @PreAuthorize). @Transactional because this
    // is two writes — the Product/ProductVariants stock update AND the
    // StockAdjustment log row — that must succeed or fail together. A crash
    // between the two would either lose the audit trail (stock changed,
    // no record of why) or log a correction that never actually applied —
    // both silently wrong, same failure shape as the TransactionItem stock
    // decrement you already fixed with @Transactional.
    @Transactional
    public StockAdjustmentResponseDTO adjustStock(StockAdjustmentRequest request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String identifier = auth.getName();
        Users currentUser = usersRepository.findByEmailOrPhoneNumber(identifier, identifier)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found: " + identifier));

        if (!"director".equals(currentUser.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only a director can manually adjust stock.");
        }

        if (request.reason() == null || request.reason().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A reason is required for every stock adjustment.");
        }

        if (request.newStock() == null || request.newStock() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "New stock must be a non-negative number.");
        }

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + request.productId()));

        // NEW: resolve the variant (if any) up front so we can check for a
        // no-op BEFORE writing anything — previously nothing stopped a
        // submission where newStock equals the current value, which wrote
        // a real audit-log row claiming a change happened when nothing
        // actually moved. Hibernate's dirty-checking silently skipped the
        // UPDATE in that case (correct behavior on its end), but the log
        // entry itself was still misleading either way.
        ProductVariants variant = null;
        if (request.productVariantId() != null) {
            variant = productVariantsRepository.findById(request.productVariantId())
                    .orElseThrow(() -> new RuntimeException(
                            "Variant not found with id: " + request.productVariantId()));
        }

        Integer currentValue = variant != null ? variant.getCurrentStock() : product.getCurrentStock();
        if (request.newStock().equals(currentValue)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "New stock value is the same as the current stock — nothing to adjust.");
        }

        StockAdjustment adjustment = new StockAdjustment();
        adjustment.setProduct(product);
        adjustment.setProductNameSnapshot(product.getName()); // NEW — captured once, survives product deletion
        adjustment.setAdjustedBy(currentUser);
        adjustment.setBranch(currentUser.getBranch());
        adjustment.setReason(request.reason());

        if (variant != null) {
            // Variant path: the variant's own currentStock is what actually moves.
            adjustment.setProductVariant(variant);
            adjustment.setPreviousStock(variant.getCurrentStock());
            adjustment.setNewStock(request.newStock());

            variant.setCurrentStock(request.newStock());
            productVariantsRepository.save(variant);
        } else {
            // Variant-less path: the product's own currentStock moves directly.
            adjustment.setPreviousStock(product.getCurrentStock());
            adjustment.setNewStock(request.newStock());

            product.setCurrentStock(request.newStock());
            productRepository.save(product);
        }

        StockAdjustment saved = stockAdjustmentRepository.save(adjustment);
        return toDTO(saved);
    }

    public List<StockAdjustmentResponseDTO> getHistoryForProduct(Long productId) {
        return stockAdjustmentRepository.findByProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public List<StockAdjustmentResponseDTO> getHistoryForVariant(Long variantId) {
        return stockAdjustmentRepository.findByProductVariantIdOrderByCreatedAtDesc(variantId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    // NEW: the deliberate "wipe everything before deployment" action —
    // separate from the automatic nullify-on-product-delete path in
    // ProductService. Director-only, same service-layer check pattern as
    // everywhere else. No confirmation step here in the service itself —
    // that responsibility belongs to the frontend (a real "type WIPE to
    // confirm" style guard, given this is irreversible and total).
    @Transactional
    public void wipeAllHistory() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String identifier = auth.getName();
        Users currentUser = usersRepository.findByEmailOrPhoneNumber(identifier, identifier)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found: " + identifier));

        if (!"director".equals(currentUser.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only a director can wipe stock adjustment history.");
        }

        stockAdjustmentRepository.deleteAllAdjustments();
    }
}