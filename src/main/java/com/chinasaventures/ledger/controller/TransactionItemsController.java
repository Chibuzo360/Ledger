package com.chinasaventures.ledger.controller;


import com.chinasaventures.ledger.model.TransactionItem;
//import com.chinasaventures.ledger.service.ProductService;
import com.chinasaventures.ledger.service.TransactionItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transaction_item")
@RequiredArgsConstructor

public class TransactionItemsController {

    private final TransactionItemService transactionItemService;

    @GetMapping
    public ResponseEntity<List<TransactionItem>> getAllTransactions(){
        return ResponseEntity.ok(transactionItemService.getAllTransactionItems());
    }

    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<List<TransactionItem>> getItemsByTransactionId(@PathVariable Long transactionId){
        return ResponseEntity.ok(transactionItemService.getItemsByTransactionId(transactionId));
    }// I fixed the problem, it was from the "getItemsBy..." in the Response entity.ok

    @GetMapping("/{id}")
    public ResponseEntity<TransactionItem> getTransactionItemsById(@PathVariable Long id){
        return ResponseEntity.ok(transactionItemService.getTransactionItemById(id));
    }

    @PostMapping
    public ResponseEntity<TransactionItem> addTransactionItem(@RequestBody TransactionItem transactionItem) {
        return ResponseEntity.ok(transactionItemService.addTransactionItem(transactionItem));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransactionItem(@PathVariable Long id){
        transactionItemService.deleteTransactionItem(id);
        return  ResponseEntity.noContent().build();
    }
}
