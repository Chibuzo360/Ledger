package com.chinasaventures.ledger.repository;

import com.chinasaventures.ledger.model.Product;
import com.chinasaventures.ledger.model.Retailers;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RetailersRepository extends JpaRepository<Retailers, Long> {
}
