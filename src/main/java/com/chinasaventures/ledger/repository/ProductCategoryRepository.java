package com.chinasaventures.ledger.repository;

import com.chinasaventures.ledger.model.Product;
import com.chinasaventures.ledger.model.ProductCategory;
import com.chinasaventures.ledger.model.ProductVariants;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long>{
}
