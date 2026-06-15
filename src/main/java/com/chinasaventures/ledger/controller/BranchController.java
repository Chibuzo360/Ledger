package com.chinasaventures.ledger.controller;

import com.chinasaventures.ledger.model.Branch;
import com.chinasaventures.ledger.service.BranchService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/branch")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    @GetMapping
    public ResponseEntity<List<Branch>> getAllBranches(){
        return ResponseEntity.ok(branchService.getAllBranch());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Branch> getBranchById( @PathVariable Long id){
        return ResponseEntity.ok(branchService.getBranchById(id));
    }

    @PostMapping
    public ResponseEntity<Branch> createBranch(@RequestBody Branch branch){
        return ResponseEntity.ok(branchService.createBranch(branch));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Branch> updateBranch(@PathVariable Long id, @RequestBody Branch updatedBranch){
        return ResponseEntity.ok(branchService.updateBranch(id,updatedBranch));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBranch(@PathVariable Long id){
        branchService.deleteBranch(id);
        return ResponseEntity.noContent().build();
    }
}
