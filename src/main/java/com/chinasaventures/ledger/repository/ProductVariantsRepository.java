package com.chinasaventures.ledger.repository;

import com.chinasaventures.ledger.model.Product;
import com.chinasaventures.ledger.model.ProductVariants;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductVariantsRepository extends JpaRepository<ProductVariants, Long>{
    List<ProductVariants> findByProductId(Long productId);
    boolean existsByProductId(Long productId);
}
