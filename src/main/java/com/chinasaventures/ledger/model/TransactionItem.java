package com.chinasaventures.ledger.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "transaction_items")
public class TransactionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transactions transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id")
    private ProductVariants productVariant;

    @Column(name = "quantity_ordered", nullable = false)
    private Integer quantityOrdered;

    @Column(name = "quantity_supplied", nullable = false)
    private Integer quantitySupplied = 0;

    @Column(name = "supply_status", nullable = false)
    private String supplyStatus = "not_supplied";

    // NEW: free-text, optional. Used when quantitySupplied < quantityOrdered
    // because the remainder is being completed elsewhere (e.g. another
    // branch) rather than backordered here. Purely informational — it
    // never affects stock math, which always runs on quantitySupplied alone.
    @Column(name = "supply_note")
    private String supplyNote;
}