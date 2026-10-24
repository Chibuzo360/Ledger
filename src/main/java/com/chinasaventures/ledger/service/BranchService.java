package com.chinasaventures.ledger.service;

import com.chinasaventures.ledger.model.Branch;
import com.chinasaventures.ledger.model.Users;
import com.chinasaventures.ledger.repository.BranchRepository;
import com.chinasaventures.ledger.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BranchService {
    private final BranchRepository branchRepository;
    private final UsersRepository usersRepository; // NEW

    // NEW: same pattern as every other role check in this codebase
    private void requireDirector(String message) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String identifier = auth.getName();
        Users currentUser = usersRepository.findByEmailOrPhoneNumber(identifier, identifier)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found: " + identifier));
        if (!"director".equals(currentUser.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
        }
    }

    public List<Branch> getAllBranch() {
        return branchRepository.findAll();
    }

    public Branch getBranchById(Long id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Branch not found with id: " + id));
    }

    public Branch createBranch(Branch branch) {
        requireDirector("Only a director can create branches."); // CHANGED
        return branchRepository.save(branch);
    }

    public Branch updateBranch(Long id, Branch updatedBranch) {
        requireDirector("Only a director can edit branches."); // CHANGED
        Branch existing = getBranchById(id);
        existing.setName(updatedBranch.getName());
        existing.setAddress(updatedBranch.getAddress());
        return branchRepository.save(existing);
    }

    public void deleteBranch(Long id) {
        requireDirector("Only a director can delete branches."); // CHANGED
        try {
            branchRepository.deleteById(id);
            branchRepository.flush(); // force the FK check here so the catch below actually fires
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This branch still has users, sales or stock linked to it and cannot be deleted.");
        }
    }
}