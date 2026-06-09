package com.chinasaventures.ledger.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "retailers")

public class Retailers {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "business_name")
    private String businessName;

    @Column(name = "contact_name")
    private String contactName;

    @Column
    private String phone;

    @Column
    private String address;

    @Column(name = "credit_limit",nullable = false)
    private BigDecimal creditLimit = BigDecimal.valueOf(0.00);

    @Column(name = "total_owed", nullable = false)
    private BigDecimal totalOwed = BigDecimal.valueOf(0.00);

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate(){createdAt = LocalDateTime.now();}
}
