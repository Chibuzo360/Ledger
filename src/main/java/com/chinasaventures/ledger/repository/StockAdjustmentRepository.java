package com.chinasaventures.ledger.repository;

import com.chinasaventures.ledger.model.StockAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
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

    // NEW: called from ProductService.deleteProduct() BEFORE the product
    // itself is deleted. Sets product to null on every adjustment row that
    // referenced it — same FK-violation problem TransactionItem had against
    // Transactions, but the fix here is "detach", not "delete", since the
    // whole point of this entity is that its history survives the product
    // going away. productNameSnapshot is what keeps these rows readable
    // afterward. @Modifying is required any time a derived/custom query
    // isn't a plain SELECT — Spring Data won't run write queries without it.
    @Modifying
    @Query("UPDATE StockAdjustment sa SET sa.product = NULL WHERE sa.product.id = :productId")
    void nullifyProductReference(Long productId);

    // NEW: the deliberate, explicit "wipe everything before deployment"
    // action — separate from anything that runs automatically on a normal
    // product delete. No filtering, no scoping: every row, gone.
    @Modifying
    @Query("DELETE FROM StockAdjustment")
    void deleteAllAdjustments();
}