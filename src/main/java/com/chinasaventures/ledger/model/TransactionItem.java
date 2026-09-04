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
    private Product product; // i added this newly " wort checking if it affects services and controllers

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id")
    private ProductVariants productVariant;


    @Column(name = "quantity_ordered", nullable = false)
    private Integer quantityOrdered;

    @Column(name = "quantity_supplied", nullable = false)
    private Integer quantitySupplied = 0;

    @Column(name = "supply_status", nullable = false)
    private String supplyStatus = "not_supplied";

    // NEW: free-text note for the "handled elsewhere" case — e.g. a customer
    // redirected to another branch to complete a partial supply. Deliberately
    // just a note, not a real per-branch stock movement — BranchStock isn't
    // wired in yet, so this can't do real cross-branch accounting, it just
    // makes the situation visible to a director reading the record.
    // Nullable — most items will never need one.
    @Column(name = "supply_note")
    private String supplyNote;
}