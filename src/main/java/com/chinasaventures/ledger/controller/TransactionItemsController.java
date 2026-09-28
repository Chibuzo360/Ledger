package com.chinasaventures.ledger.controller;

import com.chinasaventures.ledger.dto.SupplyRemainingRequest;
import com.chinasaventures.ledger.dto.TransactionItemResponseDTO;
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
    public ResponseEntity<List<TransactionItemResponseDTO>> getAllTransactions(){
        return ResponseEntity.ok(transactionItemService.getAllTransactionItems());
    }

    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<List<TransactionItemResponseDTO>> getItemsByTransactionId(@PathVariable Long transactionId){
        return ResponseEntity.ok(transactionItemService.getItemsByTransactionId(transactionId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionItemResponseDTO> getTransactionItemsById(@PathVariable Long id){
        return ResponseEntity.ok(transactionItemService.getTransactionItemByIdDTO(id));
    }

    @GetMapping("/retailer/{retailerId}")
    public ResponseEntity<List<TransactionItemResponseDTO>> getItemsByRetailerId(@PathVariable Long retailerId){
        return ResponseEntity.ok(transactionItemService.getItemsByRetailerId(retailerId));
    }

    // REMOVED: standalone POST and DELETE. Line items are now created ONLY
    // through TransactionsService.addTransaction() and removed ONLY by
    // deleting the parent transaction, so totalAmount, paymentType and
    // stock can never drift out of sync with the items.

    // Completes (fully or partially) an item that still owes units, e.g.
    // the rest of a partial delivery arriving from a later restock.
    @PutMapping("/{id}/supply")
    public ResponseEntity<TransactionItemResponseDTO> supplyRemaining(
            @PathVariable Long id,
            @RequestBody SupplyRemainingRequest request) {
        return ResponseEntity.ok(transactionItemService.supplyRemaining(id, request));
    }
}