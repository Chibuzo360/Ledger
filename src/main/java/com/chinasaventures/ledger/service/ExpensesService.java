package com.chinasaventures.ledger.service;

import com.chinasaventures.ledger.model.Expenses;
import com.chinasaventures.ledger.model.Users;
import com.chinasaventures.ledger.repository.ExpensesRepository;
import com.chinasaventures.ledger.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;


import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpensesService {
    private final ExpensesRepository expensesRepository;
    private final UsersRepository usersRepository;


    public List<Expenses> getAllExpenses() {
        return expensesRepository.findAll();
    }

    public Expenses getExpenseById(Long id){
        return expensesRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("expense record not found with id: "+ id));
    }

    public Expenses createExpense(Expenses expense){

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String identifier = auth.getName();

        Users currentUser = usersRepository.findByEmailOrPhoneNumber(identifier, identifier)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found: " + identifier));

        expense.setRecordedBy(currentUser);
        expense.setBranch(currentUser.getBranch());

        return expensesRepository.save(expense);
    }

    public Expenses updateExpenses(Long id, Expenses updatedExpenses){
        Expenses existing = getExpenseById(id);
        existing.setDescription(updatedExpenses.getDescription());
        existing.setAmount(updatedExpenses.getAmount());
        existing.setBranch(updatedExpenses.getBranch());
        return expensesRepository.save(existing);
    }

    public void deleteExpense(Long id){
        expensesRepository.deleteById(id);
    }
}
