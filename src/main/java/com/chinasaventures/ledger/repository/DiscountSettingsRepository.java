// repository/DiscountSettingsRepository.java
package com.chinasaventures.ledger.repository;

import com.chinasaventures.ledger.model.DiscountSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiscountSettingsRepository extends JpaRepository<DiscountSettings, Long> {}