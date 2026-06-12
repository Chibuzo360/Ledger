package com.chinasaventures.ledger.service;

import com.chinasaventures.ledger.model.Branch;
import com.chinasaventures.ledger.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor

public class BranchService {
    private final BranchRepository branchRepository;

    public List<Branch> getAllBranch() {
        return branchRepository.findAll();
    }

    public Branch getBranchById(Long id){
        return branchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Branch not found with id: "+ id));
    }

    public Branch createProduct(Branch branch){
        return branchRepository.save(branch);
    }

    public Branch updateBranch(Long id, Branch updatedBranch){
        Branch existing = getBranchById(id);
        existing.setName(updatedBranch.getName());
        existing.setAddress(updatedBranch.getAddress());
        return branchRepository.save(existing);
    }

    public void deleteBranch(Long id){
        branchRepository.deleteById(id);
    }
}
