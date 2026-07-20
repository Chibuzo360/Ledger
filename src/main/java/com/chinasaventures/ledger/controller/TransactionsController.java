package com.chinasaventures.ledger.controller;

import com.chinasaventures.ledger.dto.ConfirmPaymentRequest;
import com.chinasaventures.ledger.dto.TransactionResponseDTO; // CHANGED: added
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

    // CHANGED: List<Transactions> -> List<TransactionResponseDTO>
    @GetMapping
    public ResponseEntity<List<TransactionResponseDTO>> getAllTransactions() {
        return ResponseEntity.ok(transactionsService.getAllTransactions());
    }

    // CHANGED: Transactions -> TransactionResponseDTO, and call the service's private toDTO()
// via a small public wrapper (since toDTO() is private, we need the service to expose it)
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponseDTO> getTransactionById(@PathVariable Long id){
        return ResponseEntity.ok(transactionsService.getTransactionByIdDTO(id));
    }

    // CHANGED: Transactions -> TransactionResponseDTO (request body stays Transactions —
    // that's the incoming shape from the frontend form, unrelated to the response DTO)
    @PostMapping
    public ResponseEntity<TransactionResponseDTO> createTransaction(@RequestBody Transactions transaction) {
        return ResponseEntity.ok(transactionsService.addTransaction(transaction));
    }

    // CHANGED: now takes a JSON body (ConfirmPaymentRequest) instead of @RequestParam fields
    @PutMapping("/{id}/confirm")
    public ResponseEntity<TransactionResponseDTO> confirmPayment(
            @PathVariable Long id,
            @RequestBody ConfirmPaymentRequest request) {
        return ResponseEntity.ok(
                transactionsService.confirmPayment(id, request.amountPaid(), request.paymentProof()));
    }
// I added this here.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction( @PathVariable Long id){
        transactionsService.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }


}
// CHANGED: Transactions ->