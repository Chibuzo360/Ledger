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

    // NEW: CASH / POS_TRANSFER / POS_CARD / BUSINESS_ACCOUNT / MIXED.
    // Deliberately NULLABLE, not nullable = false — this table already has
    // rows from before this column existed, and a NOT NULL add on a
    // populated table is the exact discountAmount migration failure from
    // earlier. Required-ness is enforced in the service layer instead
    // (TransactionsService.addTransaction validates it's present on every
    // NEW transaction going forward), not at the DB level.
    @Column(name = "payment_method")
    private String paymentMethod;

    // NEW: free-text breakdown, used mainly when paymentMethod is MIXED
    // (e.g. "N10,000 POS transfer + N10,000 cash"). Same "simple flagging,
    // not structured tracking" pattern as TransactionItem.supplyNote —
    // readable by a human, not validated or summed by the system.
    @Column(name = "payment_method_note")
    private String paymentMethodNote;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retailer_id")
    private Retailers retailer;

    @Column(name = "discount_amount", nullable = false)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate(){createdAt = LocalDateTime.now();}
}