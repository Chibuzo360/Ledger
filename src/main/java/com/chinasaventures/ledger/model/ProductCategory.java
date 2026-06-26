package com.chinasaventures.ledger.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;


@Data
@Entity
@Table(name = "categories")
public class ProductCategory {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(nullable = false, unique = true)
        private String name;

        @Column(name = "created_at")
        private LocalDateTime createdAt;

        @PrePersist
        protected void onCreate() { createdAt = LocalDateTime.now(); }
}
