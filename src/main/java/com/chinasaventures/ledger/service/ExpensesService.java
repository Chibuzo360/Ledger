package com.chinasaventures.ledger.service;

import com.chinasaventures.ledger.dto.BranchSummaryDTO;
import com.chinasaventures.ledger.dto.TransactionResponseDTO;
import com.chinasaventures.ledger.dto.UserSummaryDTO;
import com.chinasaventures.ledger.model.Expenses;
import com.chinasaventures.ledger.model.Transactions;
import com.chinasaventures.ledger.model.Users;
import com.chinasaventures.ledger.repository.ExpensesRepository;
import com.chinasaventures.ledger.repository.UsersRepository;
import com.chinasaventures.ledger.dto.ExpensesResponseDTO;
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

    private ExpensesResponseDTO toDTO(Expenses e) {
        UserSummaryDTO recordedBy = e.getRecordedBy() != null
                ? new UserSummaryDTO(e.getRecordedBy().getId(), e.getRecordedBy().getName(), e.getRecordedBy().getRole())
                : null;
        BranchSummaryDTO branch = e.getBranch() != null
                ? new BranchSummaryDTO(e.getBranch().getId(), e.getBranch().getName())
                : null;

        return new ExpensesResponseDTO(
                e.getId(), e.getDescription(), e.getAmount(),
                recordedBy, branch, e.getCreatedAt()
        );
    }


    public List<ExpensesResponseDTO> getAllExpenses() {
        // mapped each entity to its DTO
        return expensesRepository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public Expenses getExpenseById(Long id){
        return expensesRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("expense record not found with id: "+ id));
    }

    // new method — same lookup as getExpensesById(), but returns the safe DTO

    public ExpensesResponseDTO getExpensesById(Long id){return toDTO(getExpenseById(id));}

    // Will change this to return ExpensesDTO and remove the return once im ready to change the controller.
    public Expenses createExpense(Expenses expense){

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String identifier = auth.getName();

        Users currentUser = usersRepository.findByEmailOrPhoneNumber(identifier, identifier)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found: " + identifier));

        expense.setRecordedBy(currentUser);
        expense.setBranch(currentUser.getBranch());

//        Expenses savedExpense =  expensesRepository.save(expense);
//        // This ensures it is updated before returning
//        return toDTO(savedExpense);
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
