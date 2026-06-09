package com.chinasaventures.ledger.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "transactions")

public class Transactions {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name  = "customer_phone")
    private String customerPhone;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "amount_paid", nullable = false)
    private BigDecimal amountPaid = BigDecimal.valueOf(0);

    @Column(name = "payment_status", nullable = false)
    private String paymentStatus = "pending";

    @Column(name = "payment_type", nullable = false)
    private String paymentType = "full";

    @Column(name = "payment_proof")
    private String paymentProof;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed-by")
    private Users confirmedBy;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by", nullable = false)
    private Users recordedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "retailer_id", nullable = false)
//    private Retailers retailer;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate(){createdAt = LocalDateTime.now();}
}

