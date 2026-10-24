package com.chinasaventures.ledger.config;

import com.chinasaventures.ledger.model.Branch;
import com.chinasaventures.ledger.model.Users;
import com.chinasaventures.ledger.repository.BranchRepository;
import com.chinasaventures.ledger.repository.UsersRepository;
import com.chinasaventures.ledger.service.UsersService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component // NEW
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UsersRepository usersRepository;
    private final BranchRepository branchRepository;
    private final UsersService usersService;

    @Value("${SEED_DIRECTOR_NAME:}")     private String name;
    @Value("${SEED_DIRECTOR_EMAIL:}")    private String email;
    @Value("${SEED_DIRECTOR_PHONE:}")    private String phone;
    @Value("${SEED_DIRECTOR_PASSWORD:}") private String password;
    @Value("${SEED_BRANCH_NAME:}")       private String branchName;

    @Override
    public void run(String... args) {
        if (usersRepository.count() > 0) return; // only ever bootstraps an EMPTY system

        if (name.isBlank() || password.isBlank() || (email.isBlank() && phone.isBlank())) {
            log.warn("No users exist and SEED_DIRECTOR_* env vars are not set. Nobody will be able to log in.");
            return;
        }

        Branch branch = branchRepository.findAll().stream().findFirst().orElseGet(() -> {
            Branch b = new Branch();
            b.setName(branchName.isBlank() ? "Main Branch" : branchName);
            return branchRepository.save(b);
        });

        Users director = new Users();
        director.setName(name);
        director.setEmail(email.isBlank() ? null : email);
        director.setPhoneNumber(phone.isBlank() ? null : phone);
        director.setPassword(password);
        director.setRole("director");
        director.setBranch(branch);
        usersService.createUser(director);
        log.info("Seeded first director account.");
    }
}