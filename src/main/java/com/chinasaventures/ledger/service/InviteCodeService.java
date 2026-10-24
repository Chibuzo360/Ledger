package com.chinasaventures.ledger.service;

import com.chinasaventures.ledger.dto.InviteCodeResponseDTO;
import com.chinasaventures.ledger.dto.RegisterRequest;
import com.chinasaventures.ledger.dto.UserResponseDTO;
import com.chinasaventures.ledger.model.InviteCode;
import com.chinasaventures.ledger.model.Users;
import com.chinasaventures.ledger.repository.InviteCodeRepository;
import com.chinasaventures.ledger.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InviteCodeService { // NEW

    private static final List<String> VALID_ROLES = List.of("worker", "director");
    // no I, L, O, 0, 1: easy to misread when a director reads a code out over the phone
    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 12;
    private static final long EXPIRY_HOURS = 48;
    private static final String BAD_CODE_MSG = "This invite code is invalid, already used, or expired.";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final InviteCodeRepository inviteCodeRepository;
    private final UsersRepository usersRepository;
    private final UsersService usersService;

    private Users requireDirector() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String identifier = auth.getName();
        Users currentUser = usersRepository.findByEmailOrPhoneNumber(identifier, identifier)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found: " + identifier));
        if (!"director".equals(currentUser.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only a director can generate invite codes.");
        }
        return currentUser;
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    @Transactional
    public InviteCodeResponseDTO generateCode(String role) {
        Users director = requireDirector();
        if (role == null || !VALID_ROLES.contains(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role must be one of: " + VALID_ROLES);
        }
        InviteCode invite = new InviteCode();
        invite.setCode(randomCode());
        invite.setRole(role);
        invite.setBranch(director.getBranch());   // branch comes from the director, never the client
        invite.setCreatedBy(director);
        invite.setExpiresAt(LocalDateTime.now().plusHours(EXPIRY_HOURS));
        InviteCode saved = inviteCodeRepository.save(invite);
        return new InviteCodeResponseDTO(saved.getCode(), saved.getRole(), saved.getExpiresAt());
    }

    // One transaction: if user creation fails for ANY reason, the code is NOT burned.
    @Transactional
    public UserResponseDTO registerWithCode(RegisterRequest req) {
        if (req.code() == null || req.code().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An invite code is required.");
        }
        if (req.name() == null || req.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required.");
        }
        String email = blankToNull(req.email());
        String phone = blankToNull(req.phoneNumber());
        if (email == null && phone == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide an email or a phone number.");
        }

        // Row lock: two people submitting the same code at once can't both pass this check.
        InviteCode invite = inviteCodeRepository.findByCodeForUpdate(req.code().trim().toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, BAD_CODE_MSG));

        if (invite.getUsedAt() != null || invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, BAD_CODE_MSG); // same message on purpose
        }

        Users user = new Users();
        user.setName(req.name().trim());
        user.setEmail(email);
        user.setPhoneNumber(phone);
        user.setPassword(req.password());      // createUser validates length + BCrypt-encodes
        user.setRole(invite.getRole());        // from the code, never the client
        user.setBranch(invite.getBranch());    // from the code, never the client

        try {
            Users saved = usersService.createUser(user);
            invite.setUsedBy(saved);
            invite.setUsedAt(LocalDateTime.now());
            inviteCodeRepository.save(invite);
            return usersService.toDTO(saved);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A user with that email or phone number already exists.");
        }
    }
}