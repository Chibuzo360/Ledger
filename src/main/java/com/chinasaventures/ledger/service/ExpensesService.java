package com.chinasaventures.ledger.service;

import com.chinasaventures.ledger.dto.BranchSummaryDTO;
import com.chinasaventures.ledger.dto.UserSummaryDTO;
import com.chinasaventures.ledger.model.Expenses;
import com.chinasaventures.ledger.model.Users;
import com.chinasaventures.ledger.repository.ExpensesRepository;
import com.chinasaventures.ledger.repository.UsersRepository;
import com.chinasaventures.ledger.dto.ExpensesResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpensesService {
    private final ExpensesRepository expensesRepository;
    private final UsersRepository usersRepository;

    private Users getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String identifier = auth.getName();
        return usersRepository.findByEmailOrPhoneNumber(identifier, identifier)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found: " + identifier));
    }

    private void requireDirector(String message) {
        if (!"director".equals(getCurrentUser().getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
        }
    }

    private void validateExpense(Expenses e) {
        if (e.getDescription() == null || e.getDescription().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Description is required.");
        }
        if (e.getAmount() == null || e.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be greater than zero.");
        }
    }

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
        return expensesRepository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public Expenses getExpenseById(Long id){
        return expensesRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("expense record not found with id: "+ id));
    }

    public ExpensesResponseDTO getExpensesById(Long id){return toDTO(getExpenseById(id));}

    // Open to any logged-in user (workers record day-to-day expenses).
    // CHANGED: description/amount validated.
    public ExpensesResponseDTO createExpense(Expenses expense){
        validateExpense(expense);
        Users currentUser = getCurrentUser();

        expense.setDescription(expense.getDescription().trim());
        expense.setRecordedBy(currentUser);
        expense.setBranch(currentUser.getBranch());

        Expenses savedExpense = expensesRepository.save(expense);
        return toDTO(savedExpense);
    }

    // CHANGED: director-only (previously any logged-in user could rewrite a
    // spending record) plus description/amount validation.
    public ExpensesResponseDTO updateExpenses(Long id, Expenses updatedExpenses){
        requireDirector("Only a director can edit an expense.");
        validateExpense(updatedExpenses);

        Expenses existing = getExpenseById(id);
        existing.setDescription(updatedExpenses.getDescription().trim());
        existing.setAmount(updatedExpenses.getAmount());
        Expenses updatedExpense = expensesRepository.save(existing);
        return toDTO(updatedExpense);
    }

    // CHANGED: director-only (previously any logged-in user could erase an
    // expense) and a missing id now gives a clear 404.
    public void deleteExpense(Long id){
        requireDirector("Only a director can delete an expense.");
        if (!expensesRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Expense not found with id: " + id);
        }
        expensesRepository.deleteById(id);
    }
}