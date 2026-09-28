package com.chinasaventures.ledger.repository;

import com.chinasaventures.ledger.model.StockIn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository

public interface StockInRepository extends JpaRepository<StockIn, Long>{

    // NEW: same shape as StockAdjustmentRepository's history-lookup
    // methods -- one product's (or variant's) full receiving history,
    // newest first.
    List<StockIn> findByProductIdOrderByCreatedAtDesc(Long productId);
    List<StockIn> findByProductVariantIdOrderByCreatedAtDesc(Long productVariantId);
}