package com.chinasaventures.ledger.controller;


import com.chinasaventures.ledger.model.Transactions;
import com.chinasaventures.ledger.service.TransactionsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionsController {

    private final TransactionsService transactionsService;

    @GetMapping
    public ResponseEntity<List<Transactions>> getAllTransactions(){
        return ResponseEntity.ok(transactionsService.getAllTransactions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transactions> getTransactionById(@PathVariable Long id){
        return ResponseEntity.ok(transactionsService.getTransactionById(id));
    }

    @PostMapping
    public  ResponseEntity<Transactions> createTransaction(@RequestBody Transactions transaction){
        return ResponseEntity.ok(transactionsService.addTransaction(transaction));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id){
         transactionsService.deleteTransaction(id);
         return ResponseEntity.noContent().build();
    }
}
