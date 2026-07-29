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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retailer_id")
    private Retailers retailer; // added this newly to enable fetching if a retailer bought a good

    @Column(name = "quantity_ordered", nullable = false)
    private Integer quantityOrdered;

    @Column(name = "quantity_supplied", nullable = false)
    private Integer quantitySupplied = 0;

    @Column(name = "supply_status", nullable = false)
    private String supplyStatus = "not_supplied";
}