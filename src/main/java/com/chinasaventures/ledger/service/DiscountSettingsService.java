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

    // CHANGED: now passes allowWorkerConfirmation as the DTO's third field.
    private DiscountSettingsDTO toDTO(DiscountSettings s) {
        return new DiscountSettingsDTO(s.getId(), s.getMaxDiscountAmount(), s.getAllowWorkerConfirmation());
    }

    // Fetches the single settings row, creating one with a default of N0
    // ("no discount allowed yet") if nobody has ever set a cap. Never
    // returns null or 404 -- every caller can assume this always succeeds.
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
        Users currentUser = requireDirector();

        if (newMax.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Discount cap cannot be negative.");
        }

        DiscountSettings settings = getOrCreateSettings();
        settings.setMaxDiscountAmount(newMax);
        settings.setUpdatedBy(currentUser);
        return toDTO(discountSettingsRepository.save(settings));
    }

    // NEW: director-only toggle for whether a worker (not just a director)
    // can confirm a payment on a witnessed method (cash/POS) when the
    // director isn't available. The actual enforcement of WHICH
    // transactions this applies to lives in TransactionsService.confirmPayment()
    // -- this method only controls the on/off switch itself.
    public DiscountSettingsDTO updateAllowWorkerConfirmation(Boolean allow) {
        Users currentUser = requireDirector();

        DiscountSettings settings = getOrCreateSettings();
        settings.setAllowWorkerConfirmation(Boolean.TRUE.equals(allow));
        settings.setUpdatedBy(currentUser);
        return toDTO(discountSettingsRepository.save(settings));
    }

    // NEW: extracted -- both updateMaxDiscount() and
    // updateAllowWorkerConfirmation() need the exact same director-only
    // check, so it's pulled out once instead of duplicated.
    private Users requireDirector() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String identifier = auth.getName();
        Users currentUser = usersRepository.findByEmailOrPhoneNumber(identifier, identifier)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found: " + identifier));

        if (!"director".equals(currentUser.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only a director can change this setting.");
        }
        return currentUser;
    }
}