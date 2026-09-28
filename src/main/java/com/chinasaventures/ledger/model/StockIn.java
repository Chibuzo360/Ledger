package com.chinasaventures.ledger.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "stock_in")

public class StockIn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id")
    private ProductVariants productVariant;

    // CHANGED: was @JoinColumn(name = "product") -- every other FK column
    // in this codebase uses the "_id" suffix convention (product_id,
    // branch_id, retailer_id, etc). Fixed to match. NOTE: ddl-auto:update
    // can only ADD columns, never rename -- if stock_in has any existing
    // rows, the old "product" column will be left behind, orphaned and
    // unused, while a fresh empty "product_id" column gets added. If
    // there's test data in this table, drop it and let it regenerate
    // clean, same as was done for stock_adjustments earlier.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)// At first, this will only be used for the one particular shop to prevent confusion.
    private Branch branch;

    @Column(nullable = false)
    private Integer quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by", nullable = false)
    private Users recordedBy;

    @Column(name = "truck_number")
    private String truckNumber;

    @Column(name = "delivery_note_number")
    private String deliveryNoteNumber;

    @Column(name = "supplier_name")
    private String supplierName;

    @Column
    private String note;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate(){createdAt = LocalDateTime.now();}

}