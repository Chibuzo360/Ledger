package com.chinasaventures.ledger.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// Single-row table by convention — there is only ever one of these. Not
// tied to Branch or any other entity;
// a business-wide setting like this doesn't conceptually belong to any
// existing table, so it gets its own.
@Data
@Entity
@Table(name = "discount_settings")
public class DiscountSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "max_discount_amount", nullable = false)
    private BigDecimal maxDiscountAmount = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private Users updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onSave(){ updatedAt = LocalDateTime.now(); }
}