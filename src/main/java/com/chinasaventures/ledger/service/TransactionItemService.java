package com.chinasaventures.ledger.service;

import com.chinasaventures.ledger.dto.*;
import com.chinasaventures.ledger.model.ProductVariants;
import com.chinasaventures.ledger.repository.ProductVariantsRepository;
import com.chinasaventures.ledger.repository.TransactionItemRepository;
import com.chinasaventures.ledger.model.TransactionItem;
import com.chinasaventures.ledger.model.ProductCategory;
import com.chinasaventures.ledger.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionItemService {
    private final TransactionItemRepository transactionItemRepository;
    private final ProductVariantsRepository productVariantsRepository;

    private  ProductCategory pCategory(TransactionItem item){
        //The if block  handles potential NullPointerException risks
        if (item !=null &&
                item.getProductVariant() !=null &&
                item.getProductVariant().getProduct() !=null &&
                item.getProductVariant().getProduct().getCategory() !=null) {
            return item.getProductVariant().getProduct().getCategory();
        }
        return null;
    }

    private TransactionItemResponseDTO toDTO (TransactionItem txnItem){
        RetailerSummaryDTO retailer = txnItem.getTransaction().getRetailer() !=null
                ? new RetailerSummaryDTO(
                        txnItem.getTransaction().getRetailer().getId(),
                txnItem.getTransaction().getRetailer().getBusinessName(),
                txnItem.getTransaction().getRetailer().getContactName()
        ):null;

        ProductCategoryDTO productCategory = pCategory(txnItem) !=null // used the pCategory method to make thing look good
                ? new ProductCategoryDTO(
                txnItem.getProductVariant().getProduct().getCategory().getId(),
                txnItem.getProductVariant().getProduct().getCategory().getName()
        ):null;

        ProductSummaryDTO product = txnItem.getProduct() !=null
                ? new ProductSummaryDTO(
                txnItem.getProduct().getId(),
                txnItem.getProduct().getName(),
                productCategory,
                txnItem.getProduct().getCurrentStock()
        ): null;

        ProductVariantSummaryDTO productVariant = txnItem.getProductVariant() !=null
                ?new ProductVariantSummaryDTO(
                        txnItem.getProductVariant().getId(),
                product,
                txnItem.getProductVariant().getSize(),
                txnItem.getProductVariant().getProducer(),
                txnItem.getProductVariant().getCurrentStock()
        ):null;

        TransactionSummaryDTO transaction = txnItem.getTransaction() !=null
                ? new TransactionSummaryDTO(
                        txnItem.getTransaction().getId(),
                txnItem.getTransaction().getCustomerName(),
                txnItem.getTransaction().getTotalAmount(),
                txnItem.getTransaction().getAmountPaid(),
                retailer)
                :null;



    }

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

    public List<TransactionItem> getItemsByRetailerId(Long retailerId){
        return transactionItemRepository.findByTransaction_Retailer_Id(retailerId);
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
