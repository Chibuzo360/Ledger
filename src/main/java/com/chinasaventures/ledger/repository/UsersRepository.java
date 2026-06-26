package com.chinasaventures.ledger.repository;

import com.chinasaventures.ledger.model.Product;
import com.chinasaventures.ledger.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository

public interface UsersRepository extends JpaRepository<Users, Long>{


    Optional<Users> findByEmailOrPhoneNumber(String email, String phoneNumber);

//    Optional<Users> findByEmail(String email);
//    Optional<Users> findByPhoneNumber(String phoneNumber);
}
