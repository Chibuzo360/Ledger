package com.chinasaventures.ledger.service;

import com.chinasaventures.ledger.model.Expenses;
import com.chinasaventures.ledger.repository.ExpensesRepository;
//import com.chinasaventures.ledger.model.Branch;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpensesService {
    private final ExpensesRepository expensesRepository;

    public List<Expenses> getAllExpenses() {
        return expensesRepository.findAll();
    }

    public Expenses getExpenseById(Long id){
        return expensesRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("expense record not found with id: "+ id));
    }

    public Expenses createExpense(Expenses expense){
        return expensesRepository.save(expense);
    }

    public Expenses updateExpenses(Long id, Expenses updatedExpenses){
        Expenses existing = getExpenseById(id);
        existing.setDescription(updatedExpenses.getDescription());
        existing.setAmount(updatedExpenses.getAmount());
        existing.setBranch(updatedExpenses.getBranch());
        return expensesRepository.save(existing);
    }

    public void deleteBranch(Long id){
        expensesRepository.deleteById(id);
    }
}
