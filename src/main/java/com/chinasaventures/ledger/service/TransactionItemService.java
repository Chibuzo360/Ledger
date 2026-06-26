package com.chinasaventures.ledger.service;

import com.chinasaventures.ledger.model.ProductVariants;
import com.chinasaventures.ledger.repository.ProductVariantsRepository;
import com.chinasaventures.ledger.repository.TransactionItemRepository;
import com.chinasaventures.ledger.model.TransactionItem;
import com.chinasaventures.ledger.model.Product;
import com.chinasaventures.ledger.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionItemService {
    private final TransactionItemRepository transactionItemRepository;
    private final ProductVariantsRepository productVariantsRepository;

    public List<TransactionItem> getAllTransactionItems(){
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

        ProductVariants product = saved.getProductVariant();
        product.setCurrentStock(product.getCurrentStock() - saved.getQuantitySupplied());
        productVariantsRepository.save(product);
        return saved;
    }

    public void deleteTransactionItem(Long id){
        transactionItemRepository.deleteById(id);
    }

}
