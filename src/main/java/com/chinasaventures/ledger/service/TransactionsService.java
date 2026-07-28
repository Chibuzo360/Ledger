package com.chinasaventures.ledger.service;

import com.chinasaventures.ledger.dto.RetailerSummaryDTO;
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
        RetailerSummaryDTO retailer = t.getRetailer() != null
                ? new RetailerSummaryDTO(t.getRetailer().getId(), t.getRetailer().getBusinessName())
                : null;

        return new TransactionResponseDTO(
                t.getId(), t.getCustomerName(), t.getCustomerPhone(),
                t.getTotalAmount(), t.getAmountPaid(), t.getPaymentStatus(),
                t.getPaymentType(), t.getPaymentProof(),
                recordedBy, confirmedBy,retailer , t.getConfirmedAt(), t.getCreatedAt()
        );
    }

    public List<TransactionResponseDTO> getAllTransactions() {
        // swapped findAll() for the ordered version (the last created stays up)
        return transactionsRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    // kept this one returning the raw entity — confirmPayment() and addTransaction()
    // still need the real Transactions object internally (e.g. to save it), so this stays
    // as an internal helper. Controller will call toDTO() on the result before responding.
    public Transactions getTransactionById(Long id){
        return transactionsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found with id: "+ id));
    }

    // new method — same lookup as getTransactionById(), but returns the safe DTO
    // for controller use. getTransactionById() stays as-is since confirmPayment() needs
    // the raw entity internally to modify and save it.
    public TransactionResponseDTO getTransactionByIdDTO(Long id) {
        return toDTO(getTransactionById(id));
    }

    // return type TransactionResponseDTO instead of Transactions
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

    // CHANGED: confirmPayment now validates the cap before setting amountPaid
    public TransactionResponseDTO confirmPayment(Long id, BigDecimal amountPaid, String paymentProof) {
        Transactions transaction = getTransactionById(id);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String identifier = auth.getName();
        Users currentUser = usersRepository.findByEmailOrPhoneNumber(identifier, identifier)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found: " + identifier));

        if (amountPaid != null) {
            // NEW: guard — can't record more than what's actually owed
            if (amountPaid.compareTo(transaction.getTotalAmount()) > 0) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        "Amount paid cannot exceed total amount owed"
                );
            }
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

        // In TransactionsService.addTransaction() — CHANGED: check if the frontend already marked this confirmed
        if ("confirmed".equalsIgnoreCase(transaction.getPaymentStatus())) {
            transaction.setPaymentStatus("confirmed");
            transaction.setConfirmedBy(currentUser);
            transaction.setConfirmedAt(LocalDateTime.now());
        } else {
            transaction.setPaymentStatus("pending"); // unchanged default
        }
        transaction.setPaymentProof(paymentProof);
        transaction.setConfirmedAt(LocalDateTime.now());
        transaction.setConfirmedBy(currentUser);

        Transactions saved = transactionsRepository.save(transaction);
        return toDTO(saved);
    }

    // CHANGED: deleteTransaction now checks who's deleting and what state the transaction is in
    public void deleteTransaction(Long id){
        Transactions transaction = getTransactionById(id);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String identifier = auth.getName();
        Users currentUser = usersRepository.findByEmailOrPhoneNumber(identifier, identifier)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found: " + identifier));

        // NEW: confirmed transactions can only be deleted by a director
        if ("confirmed".equals(transaction.getPaymentStatus()) && !"director".equals(currentUser.getRole())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Only a director can delete a confirmed transaction"
            );
        }

        transactionsRepository.deleteById(id);
    }
}