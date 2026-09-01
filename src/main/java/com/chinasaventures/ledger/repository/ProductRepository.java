package com.chinasaventures.ledger.repository;

import com.chinasaventures.ledger.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository

public interface ProductRepository extends JpaRepository<Product, Long>{
    List<Product> findByCategoryId(Long categoryId);
    boolean existsByCategoryId(Long categoryId);
}
