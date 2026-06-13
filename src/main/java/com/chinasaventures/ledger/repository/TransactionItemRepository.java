package com.chinasaventures.ledger.repository;

import com.chinasaventures.ledger.model.Product;
import com.chinasaventures.ledger.model.TransactionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository

public interface TransactionItemRepository extends JpaRepository<TransactionItem, Long>{
    List<TransactionItem> findByTransactionId(Long transactionId);
}
