//package com.chinasaventures.ledger.model;
//
//import jakarta.persistence.*;
//import lombok.Data;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//
//@Data
//@Entity
//@Table(name = "master_stock")
//
//public class BranchStock {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private  Long id;
//
//    @Column(nullable = false, name = "product_name")
//    private String productName;
//
//    @Column(nullable = false)
//    private String unit;
//
//   @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "branch_id", nullable = false)
//    private Branch branch;
//
//    @Column(name = "total_current_stock")
//    private Integer totalCurrentStock = 0;
//
//
//    @Column(name = "created_at")
//    private LocalDateTime createdAt;
//
//    @PrePersist
//    protected void onCreate(){
//        createdAt = LocalDateTime.now();
//    }
//
//
//}

//THIS FILE IS INTENDED FOR TESTING THE MASTER LEDGER WHERE THE TOTAL GOODS AVAILABLE FOR ALL BRANCHES CAN BE SEEN.
