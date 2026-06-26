package com.chinasaventures.ledger.security;

import com.chinasaventures.ledger.model.Users;
import com.chinasaventures.ledger.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsersRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        // 1. Fetch by Email OR Phone Number
        Users domainUser = userRepository.findByEmailOrPhoneNumber(identifier, identifier)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with identifier: " + identifier));

        // This guarantees jwtUtil.isTokenValid() evaluates true whether it's an email or phone number!
        return org.springframework.security.core.userdetails.User.builder()
                .username(identifier)
                .password(domainUser.getPassword())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + domainUser.getRole().toUpperCase())))
                .build();
    }
}