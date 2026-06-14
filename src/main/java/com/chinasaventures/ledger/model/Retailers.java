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
    private BigDecimal creditLimit = BigDecimal.valueOf(0.00);// I might remove this later.

    @Column(name = "balance", nullable = false)
    private BigDecimal balance = BigDecimal.valueOf(0.00);

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate(){createdAt = LocalDateTime.now();}

    // we could add the total amount the customer has spent in the business
    // we could also add the last payment date and the last payment amount.
    //
}
