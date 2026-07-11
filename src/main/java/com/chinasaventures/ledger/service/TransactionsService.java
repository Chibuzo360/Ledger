package com.chinasaventures.ledger.service;

import com.chinasaventures.ledger.dto.TransactionResponseDTO;
import com.chinasaventures.ledger.dto.UserSummaryDTO;
import com.chinasaventures.ledger.model.Transactions;
import com.chinasaventures.ledger.repository.TransactionItemRepository;
import com.chinasaventures.ledger.repository.TransactionsRepository;
import com.chinasaventures.ledger.repository.ProductRepository;
import com.chinasaventures.ledger.repository.UsersRepository;
import com.chinasaventures.ledger.model.Users;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionsService {
    private final TransactionsRepository transactionsRepository;
    private final TransactionItemRepository transactionItemRepository;
    private final ProductRepository productRepository;
    private final UsersRepository usersRepository;

    private TransactionResponseDTO toDTO(Transactions t) {
        UserSummaryDTO recordedBy = t.getRecordedBy() != null
                ? new UserSummaryDTO(t.getRecordedBy().getId(), t.getRecordedBy().getName(), t.getRecordedBy().getRole())
                : null;
        UserSummaryDTO confirmedBy = t.getConfirmedBy() != null
                ? new UserSummaryDTO(t.getConfirmedBy().getId(), t.getConfirmedBy().getName(), t.getConfirmedBy().getRole())
                : null;

        return new TransactionResponseDTO(
                t.getId(), t.getCustomerName(), t.getCustomerPhone(),
                t.getTotalAmount(), t.getAmountPaid(), t.getPaymentStatus(),
                t.getPaymentType(), t.getPaymentProof(),
                recordedBy, confirmedBy, t.getConfirmedAt(), t.getCreatedAt()
        );
    }

    // CHANGED: return type List<TransactionResponseDTO> instead of List<Transactions>
    public List<TransactionResponseDTO> getAllTransactions() {
        // CHANGED: swapped findAll() for the ordered version
        return transactionsRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    // CHANGED: kept this one returning the raw entity — confirmPayment() and addTransaction()
    // still need the real Transactions object internally (e.g. to save it), so this stays
    // as an internal helper. Controller will call toDTO() on the result before responding.
    public Transactions getTransactionById(Long id){
        return transactionsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found with id: "+ id));
    }

    // CHANGED: new method — same lookup as getTransactionById(), but returns the safe DTO
    // for controller use. getTransactionById() stays as-is since confirmPayment() needs
    // the raw entity internally to modify and save it.
    public TransactionResponseDTO getTransactionByIdDTO(Long id) {
        return toDTO(getTransactionById(id));
    }

    // CHANGED: return type TransactionResponseDTO instead of Transactions
    public TransactionResponseDTO addTransaction(Transactions transaction){

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String identifier = auth.getName();

        Users currentUser = usersRepository.findByEmailOrPhoneNumber(identifier, identifier)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found: " + identifier));

        transaction.setRecordedBy(currentUser);
        transaction.setBranch(currentUser.getBranch());

        BigDecimal debt = transaction.getTotalAmount()
                .subtract(transaction.getAmountPaid());

        if(debt.compareTo(transaction.getTotalAmount()) == 0) {
            transaction.setPaymentType("credit");
        } else if(debt.compareTo(BigDecimal.ZERO) > 0) {
            transaction.setPaymentType("part_payment");
        } else {
            transaction.setPaymentType("full");
        }

        transaction.setPaymentStatus("pending");

        Transactions saved = transactionsRepository.save(transaction);
        return toDTO(saved); // CHANGED: map before returning
    }

    // CHANGED: return type TransactionResponseDTO instead of Transactions
    // CHANGED: confirmPayment now takes the new amount + proof, pulls confirmedBy from the JWT
// (same pattern as addTransaction), and recalculates paymentType if the amount changed
    public TransactionResponseDTO confirmPayment(Long id, BigDecimal amountPaid, String paymentProof) {
        Transactions transaction = getTransactionById(id);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String identifier = auth.getName();
        Users currentUser = usersRepository.findByEmailOrPhoneNumber(identifier, identifier)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found: " + identifier));

        // NEW: only touch amountPaid/paymentType if a new amount was actually sent
        if (amountPaid != null) {
            transaction.setAmountPaid(amountPaid);

            BigDecimal debt = transaction.getTotalAmount().subtract(amountPaid);
            if (debt.compareTo(transaction.getTotalAmount()) == 0) {
                transaction.setPaymentType("credit");
            } else if (debt.compareTo(BigDecimal.ZERO) > 0) {
                transaction.setPaymentType("part_payment");
            } else {
                transaction.setPaymentType("full");
            }
        }

        transaction.setPaymentStatus("confirmed");
        transaction.setPaymentProof(paymentProof);
        transaction.setConfirmedAt(LocalDateTime.now());
        transaction.setConfirmedBy(currentUser); // CHANGED: from JWT, not request param

        Transactions saved = transactionsRepository.save(transaction);
        return toDTO(saved);
    }

    public void deleteTransaction(Long id){
        transactionsRepository.deleteById(id);
    }
}