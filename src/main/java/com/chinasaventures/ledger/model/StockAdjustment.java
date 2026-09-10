package com.chinasaventures.ledger.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

// NEW ENTITY: immutable audit log for manual director-only stock corrections.
// Same philosophy as StockIn — no update endpoint, ever. A correction to a
// correction is a NEW row, not an edit to this one. previousStock/newStock
// are both stored (not just a delta) so a director scanning history later
// sees the full before/after picture at a glance, not just "+/- 7" with no
// context of what the number actually was.
@Entity
@Table(name = "stock_adjustments")
@Data
public class StockAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Optional — same nullable pattern as StockIn.productVariant, since not
    // every product has variants.
    @ManyToOne
    @JoinColumn(name = "product_variant_id")
    private ProductVariants productVariant;

    @Column(nullable = false)
    private Integer previousStock;

    @Column(nullable = false)
    private Integer newStock;

    // The whole point of this entity. Required — an unreasoned stock jump
    // is indistinguishable from a hidden mistake.
    @Column(nullable = false)
    private String reason;

    // Derived from SecurityContextHolder in the service layer, never
    // trusted from the client — same pattern as recordedBy elsewhere.
    @ManyToOne
    @JoinColumn(name = "adjusted_by", nullable = false)
    private Users adjustedBy;

    @ManyToOne
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}