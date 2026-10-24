package com.chinasaventures.ledger.repository;

import com.chinasaventures.ledger.model.InviteCode;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InviteCodeRepository extends JpaRepository<InviteCode, Long> { // ID type is Long
    Optional<InviteCode> findByCode(String code);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InviteCode i where i.code = :code")
    Optional<InviteCode> findByCodeForUpdate(@Param("code") String code);
}