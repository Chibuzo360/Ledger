package com.chinasaventures.ledger.repository;

import com.chinasaventures.ledger.model.Branch;
import com.chinasaventures.ledger.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository

public interface BranchRepository extends JpaRepository<Branch, Long> {
}
