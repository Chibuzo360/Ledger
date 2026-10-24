package com.chinasaventures.ledger.service;

import com.chinasaventures.ledger.dto.BranchSummaryDTO;
import com.chinasaventures.ledger.dto.UserResponseDTO;
import com.chinasaventures.ledger.model.Users;
import com.chinasaventures.ledger.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsersService {

    private static final List<String> VALID_ROLES = List.of("worker", "director");
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UsersRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ---- helpers -------------------------------------------------------

    private Users requireDirector(String message) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String identifier = auth.getName();
        Users currentUser = userRepository.findByEmailOrPhoneNumber(identifier, identifier)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found: " + identifier));
        if (!"director".equals(currentUser.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
        }
        return currentUser;
    }

    public UserResponseDTO toDTO(Users u) {// I changed this to public because InvitecodeService needs it
        BranchSummaryDTO branch = u.getBranch() != null
                ? new BranchSummaryDTO(u.getBranch().getId(), u.getBranch().getName())
                : null;
        return new UserResponseDTO(
                u.getId(), u.getName(), u.getEmail(), u.getPhoneNumber(), u.getRole(), branch
        );
    }

    private void validateRole(String role) {
        if (role == null || !VALID_ROLES.contains(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Role must be one of: " + VALID_ROLES);
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
    }

    // ---- reads (director-only; return DTOs, never the entity) ------------

    public List<UserResponseDTO> getAllUsers() {
        requireDirector("Only a director can view users.");
        return userRepository.findAll().stream().map(this::toDTO).toList();
    }

    // Internal lookup, returns the entity. NOT exposed by any endpoint.
    public Users getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User with id: " + id + ", was not found"));
    }

    public UserResponseDTO getUserDTOById(Long id) {
        requireDirector("Only a director can view users.");
        return toDTO(getUserById(id));
    }

    // ---- create ---------------------------------------------------------

    // UNCHANGED SIGNATURE and still ungated on purpose: this is the method
    // the public /api/auth/register bootstrap flow calls, and gating it
    // would make it impossible to create the very first director. The
    // standalone POST /api/users goes through createUserAsDirector() below.
    // NEW: role and password-length validation (applies to register too).
    public Users createUser(Users user) {
        validateRole(user.getRole());
        validatePassword(user.getPassword());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    // NEW: director-only entry point for POST /api/users.
    public UserResponseDTO createUserAsDirector(Users user) {
        requireDirector("Only a director can create users.");
        try {
            return toDTO(createUser(user));
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A user with that email or phone number already exists, or a required field is missing.");
        }
    }

    // ---- update ---------------------------------------------------------

    // CHANGED: director-only. Previously ANY logged-in worker could set any
    // user's role to "director". A null field in the request means
    // "leave unchanged" (so a partial body can no longer wipe a required
    // column). A director cannot change their OWN role, so the last
    // director can't accidentally lock everyone out.
    public UserResponseDTO updateUsers(Long id, Users updatedUsers) {
        Users currentUser = requireDirector("Only a director can edit users.");
        Users existing = getUserById(id);

        if (updatedUsers.getRole() != null) {
            validateRole(updatedUsers.getRole());
            if (existing.getId().equals(currentUser.getId())
                    && !updatedUsers.getRole().equals(existing.getRole())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "You cannot change your own role.");
            }
            existing.setRole(updatedUsers.getRole());
        }
        if (updatedUsers.getName() != null) {
            if (updatedUsers.getName().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name cannot be blank.");
            }
            existing.setName(updatedUsers.getName().trim());
        }
        if (updatedUsers.getPhoneNumber() != null) existing.setPhoneNumber(updatedUsers.getPhoneNumber());
        if (updatedUsers.getEmail() != null) existing.setEmail(updatedUsers.getEmail());
        if (updatedUsers.getBranch() != null) existing.setBranch(updatedUsers.getBranch());

        try {
            return toDTO(userRepository.save(existing));
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Another user already has that email or phone number.");
        }
    }

    // CHANGED: director-only, min length enforced, returns a DTO. Still
    // never stores plaintext: the password is BCrypt-encoded first.
    public UserResponseDTO updatePassword(Long id, String newPassword) {
        requireDirector("Only a director can reset a password.");
        validatePassword(newPassword);
        Users existing = getUserById(id);
        existing.setPassword(passwordEncoder.encode(newPassword));
        return toDTO(userRepository.save(existing));
    }

    // ---- delete ---------------------------------------------------------

    // CHANGED: director-only; cannot delete yourself; a user who has
    // recorded sales/expenses/stock cannot be deleted (409 with a clear
    // message) instead of a raw foreign-key 500.
    public void deleteUser(Long id) {
        Users currentUser = requireDirector("Only a director can delete users.");
        Users existing = getUserById(id);

        if (existing.getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "You cannot delete your own account.");
        }
        try {
            userRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This user has recorded transactions, expenses or stock activity and cannot be deleted.");
        }
    }
}