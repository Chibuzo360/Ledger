package com.chinasaventures.ledger.repository;

import com.chinasaventures.ledger.model.Expenses;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpensesRepository  extends JpaRepository<Expenses, Long> {
}
