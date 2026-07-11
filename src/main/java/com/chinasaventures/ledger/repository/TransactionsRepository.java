package com.chinasaventures.ledger.repository;

import com.chinasaventures.ledger.model.Product;
import com.chinasaventures.ledger.model.Transactions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository

public interface TransactionsRepository extends JpaRepository<Transactions, Long>{
    List<Transactions> findAllByOrderByCreatedAtDesc();
    Long id(Long id);
}
