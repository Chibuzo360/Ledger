package com.chinasaventures.ledger.controller;

import com.chinasaventures.ledger.dto.ExpensesResponseDTO;
import com.chinasaventures.ledger.model.Expenses;
import com.chinasaventures.ledger.service.ExpensesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpensesController {
    private final ExpensesService expensesService;

    @GetMapping
    public ResponseEntity<List<ExpensesResponseDTO>> getAllExpenses() {
        return ResponseEntity.ok(expensesService.getAllExpenses());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExpensesResponseDTO> getExpensesById(@PathVariable Long id){
        return ResponseEntity.ok(expensesService.getExpensesById(id));
    }

    @PostMapping
    public ResponseEntity<ExpensesResponseDTO> createExpense(@RequestBody Expenses expense){
        return ResponseEntity.ok(expensesService.createExpense(expense));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExpensesResponseDTO> updateExpenses(@PathVariable Long id, @RequestBody Expenses updatedExpenses){
        return ResponseEntity.ok(expensesService.updateExpenses(id, updatedExpenses));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id){
        expensesService.deleteExpense(id);
        return ResponseEntity.noContent().build();
    }
}
