package com.chinasaventures.ledger.controller;

import com.chinasaventures.ledger.dto.RegisterRequest;           // NEW
import com.chinasaventures.ledger.dto.UserResponseDTO;           // NEW
import com.chinasaventures.ledger.model.Users;
import com.chinasaventures.ledger.repository.UsersRepository;
import com.chinasaventures.ledger.security.JwtUtil;
import com.chinasaventures.ledger.service.InviteCodeService;     // NEW
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;                      // NEW
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UsersRepository usersRepository;
    private final JwtUtil jwtUtil;
    private final InviteCodeService inviteCodeService;           // CHANGED: replaces UsersService

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getIdentifier(),
                        loginRequest.getPassword()
                )
        );

        Users user = usersRepository.findByEmailOrPhoneNumber(
                        loginRequest.getIdentifier(), loginRequest.getIdentifier())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtUtil.generateToken(loginRequest.getIdentifier(), user.getRole());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("role", user.getRole());
        response.put("identifier", user.getName());

        return ResponseEntity.ok(response);
    }

    // CHANGED: takes RegisterRequest instead of the Users entity, returns a DTO instead of the entity
    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inviteCodeService.registerWithCode(request));
    }

    @Data
    public static class LoginRequest {
        private String identifier;
        private String password;
    }
}