//package com.chinasaventures.ledger.model;
//
//import jakarta.persistence.*;
//import lombok.Data;
//
//@Data
//@Entity
//@Table(name = "branch_stock")
//public class BranchStock {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "branch_id", nullable = false)
//    private Branch branch;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "product_id", nullable = false)
//    private Product product;
//
//    @Column(nullable = false)
//    private Integer quantity = 0;
//}