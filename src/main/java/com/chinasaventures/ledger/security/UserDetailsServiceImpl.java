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
        // 1. Fetch by Email OR Phone Number, if the fetch is successful,
        // it the maps it to the "UserDetails" format which is the format spring understands.

        Users domainUser = userRepository.findByEmailOrPhoneNumber(identifier, identifier)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with identifier: " + identifier));

        // This guarantees jwtUtil.isTokenValid() evaluates true whether it's an email or phone number!

        return org.springframework.security.core.userdetails.User.builder()
                .username(identifier) // Keeps whatever the user typed in (email or phone)
                .password(domainUser.getPassword()) // Fetches the hashed password from DB
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + domainUser.getRole().toUpperCase())))
                .build();
        //Collections.singletonList(...): Creates an immutable list containing just this one role,
        // converting it into a SimpleGrantedAuthority object that Spring Security requires.
        // domainUser.getRole().toUpperCase(): Fetches the role from your database (e.g., "admin") and forces it to uppercase ("ADMIN").
        //"ROLE_" + ...: Spring Security strictly expects roles to be prefixed with ROLE_ by default (resulting in "ROLE_ADMIN").
        // .build();
        //This is the final stamp. It takes all the pieces you just configured and compiles them into a completed UserDetails object,
        // which is then returned to the Spring Security authentication manager.
    }
}