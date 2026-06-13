package com.chinasaventures.ledger.service;


import com.chinasaventures.ledger.model.Transactions;
import com.chinasaventures.ledger.repository.TransactionItemRepository;
import com.chinasaventures.ledger.repository.TransactionsRepository;
import com.chinasaventures.ledger.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionsService {
    private final TransactionsRepository transactionsRepository;
    private final TransactionItemRepository transactionItemRepository;
    private final ProductRepository productRepository;

    public List<Transactions> getAllTransactions() {
        return transactionsRepository.findAll();
    }

    public Transactions getTransactionById(Long id){
        return transactionsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found with id: "+ id));
    }

    public Transactions createTransaction(Transactions transaction){

        BigDecimal debt = transaction.getTotalAmount()
                .subtract(transaction.getAmountPaid());

        if(debt.compareTo(transaction.getTotalAmount()) == 0) {
            transaction.setPaymentType("credit");
        } else if(debt.compareTo(BigDecimal.ZERO) > 0) {
            transaction.setPaymentType("part_payment");
        } else {
            transaction.setPaymentType("full");
        }

        return transactionsRepository.save(transaction);
    }

    public void deleteTransaction(Long id){
        transactionsRepository.deleteById(id);
    }
}
