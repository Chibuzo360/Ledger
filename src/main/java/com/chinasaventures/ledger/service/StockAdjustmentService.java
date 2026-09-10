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

    private StockAdjustmentResponseDTO toDTO(StockAdjustment a) {
        Product p = a.getProduct();
        ProductVariants v = a.getProductVariant();

        String variantDescription = v != null
                ? v.getSize() + " - " + v.getProducer()
                : null;

        return new StockAdjustmentResponseDTO(
                a.getId(),
                p.getId(),
                p.getName(),
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

        StockAdjustment adjustment = new StockAdjustment();
        adjustment.setProduct(product);
        adjustment.setAdjustedBy(currentUser);
        adjustment.setBranch(currentUser.getBranch());
        adjustment.setReason(request.reason());

        if (request.productVariantId() != null) {
            // Variant path: the variant's own currentStock is what actually moves.
            ProductVariants variant = productVariantsRepository.findById(request.productVariantId())
                    .orElseThrow(() -> new RuntimeException(
                            "Variant not found with id: " + request.productVariantId()));

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
}