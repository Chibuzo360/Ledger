package com.chinasaventures.ledger.service;

import com.chinasaventures.ledger.dto.DiscountSettingsDTO;
import com.chinasaventures.ledger.model.DiscountSettings;
import com.chinasaventures.ledger.model.Users;
import com.chinasaventures.ledger.repository.DiscountSettingsRepository;
import com.chinasaventures.ledger.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DiscountSettingsService {
    private final DiscountSettingsRepository discountSettingsRepository;
    private final UsersRepository usersRepository;

    private DiscountSettingsDTO toDTO(DiscountSettings s) {
        return new DiscountSettingsDTO(s.getId(), s.getMaxDiscountAmount());
    }

    // Fetches the single settings row, creating one with a default of ₦0
    // ("no discount allowed yet") if nobody has ever set a cap. Never
    // returns null or 404 — every caller can assume this always succeeds.
    public DiscountSettings getOrCreateSettings() {
        List<DiscountSettings> all = discountSettingsRepository.findAll();
        if (!all.isEmpty()) return all.get(0);

        DiscountSettings fresh = new DiscountSettings();
        fresh.setMaxDiscountAmount(BigDecimal.ZERO);
        return discountSettingsRepository.save(fresh);
    }

    public DiscountSettingsDTO getSettingsDTO() {
        return toDTO(getOrCreateSettings());
    }

    // Director-only, same enforcement pattern as every other role check in
    // this codebase (checked in the service, not via @PreAuthorize).
    public DiscountSettingsDTO updateMaxDiscount(BigDecimal newMax) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String identifier = auth.getName();
        Users currentUser = usersRepository.findByEmailOrPhoneNumber(identifier, identifier)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found: " + identifier));

        if (!"director".equals(currentUser.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only a director can change the discount cap.");
        }
        if (newMax.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Discount cap cannot be negative.");
        }

        DiscountSettings settings = getOrCreateSettings();
        settings.setMaxDiscountAmount(newMax);
        settings.setUpdatedBy(currentUser);
        return toDTO(discountSettingsRepository.save(settings));
    }
}