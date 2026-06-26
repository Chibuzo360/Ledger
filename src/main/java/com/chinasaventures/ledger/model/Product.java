package com.chinasaventures.ledger.model;

import jakarta.persistence.*;
import lombok.Data;


import java.time.LocalDateTime;


@Data
@Entity
@Table(name = "products")

public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private  Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String unit;

//    @Column(nullable = false)
//    private BigDecimal pricePerUnit = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ProductCategory category; // Cement, Rod, Tiles.

    @Column(name = "current_stock")
    private Integer currentStock = 0;


    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate(){
        createdAt = LocalDateTime.now();
    }


}
