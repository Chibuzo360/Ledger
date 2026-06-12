package com.chinasaventures.ledger.repository;

import com.chinasaventures.ledger.model.Product;
import com.chinasaventures.ledger.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository

public interface UsersRepository extends JpaRepository<Users, Long>{
}
