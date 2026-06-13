package com.chinasaventures.ledger.service;

import com.chinasaventures.ledger.repository.TransactionItemRepository;
import com.chinasaventures.ledger.model.TransactionItem;
import com.chinasaventures.ledger.model.Product;
import com.chinasaventures.ledger.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TranscationItemService {
    private final TransactionItemRepository transactionItemRepository;
    private final ProductRepository productRepository;

    public List<TransactionItem> getAllTransactionItem(){
        return transactionItemRepository.findAll();
    }

    public List<TransactionItem> getItemsByTransactionId(Long transactionId){
        return transactionItemRepository.findByTransactionId(transactionId);
    }

    public TransactionItem getTransactionItemById(Long id){
        return transactionItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Item not found with id: " + id));
    }

    public TransactionItem addTransactionItem(TransactionItem transactionItem){
        TransactionItem saved = transactionItemRepository.save(transactionItem);

        Product product = saved.getProduct();
        product.setCurrentStock(product.getCurrentStock() - saved.getQuantitySupplied());
        productRepository.save(product);
        return saved;
    }

    public void deleteTransactionItem(Long id){
        transactionItemRepository.deleteById(id);
    }

}
