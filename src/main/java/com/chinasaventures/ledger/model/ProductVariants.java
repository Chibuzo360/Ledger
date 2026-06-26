package com.chinasaventures.ledger.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "product_variants")
public class ProductVariants {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String size; // '40x40', '60x60', '12mm'

    @Column
    private String producer; // 'Lafarge', 'Virony', 'Dangote'

    @Column(name = "price_per_unit", nullable = false)
    private BigDecimal pricePerUnit = BigDecimal.ZERO;

    @Column(name = "current_stock")
    private Integer currentStock = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}

