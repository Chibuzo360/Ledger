package com.chinasaventures.ledger.repository;

import com.chinasaventures.ledger.model.StockAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// NEW
@Repository
public interface StockAdjustmentRepository extends JpaRepository<StockAdjustment, Long> {

    // History for one product's audit trail (variant-less products, or the
    // "all adjustments under this product" view).
    List<StockAdjustment> findByProductIdOrderByCreatedAtDesc(Long productId);

    // History for one specific variant.
    List<StockAdjustment> findByProductVariantIdOrderByCreatedAtDesc(Long productVariantId);
}