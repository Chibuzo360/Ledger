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
                item.getProduct() !=null &&
                item.getProduct().getCategory() !=null) {
            return item.getProduct().getCategory();
        }
        return null;
    }

    private TransactionItemResponseDTO toDTO (TransactionItem txnItem){
        RetailerSummaryDTO retailer = txnItem.getRetailer() !=null
                ? new RetailerSummaryDTO(
                        txnItem.getRetailer().getId(),
                txnItem.getRetailer().getBusinessName(),
                txnItem.getRetailer().getContactName()
        ):null;

        ProductCategoryDTO productCategory = pCategory(txnItem) !=null // used the pCategory method to make thing look good
                ? new ProductCategoryDTO(
                pCategory(txnItem).getId(),
                pCategory(txnItem).getName()
        ):null;

        TransactionSummaryDTO transaction = txnItem.getTransaction() !=null
                ? new TransactionSummaryDTO(
                txnItem.getTransaction().getId(),
                txnItem.getTransaction().getCustomerName(),
                txnItem.getTransaction().getTotalAmount(),
                txnItem.getTransaction().getAmountPaid(),
                retailer)
                :null;

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

        return new TransactionItemResponseDTO(
                txnItem.getId(), transaction, product, productVariant,
                txnItem.getQuantityOrdered(), txnItem.getQuantitySupplied(),
                txnItem.getSupplyStatus()
        );
    }

    public List<TransactionItemResponseDTO> getAllTransactionItems(){
        return transactionItemRepository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }


    public List<TransactionItemResponseDTO> getItemsByTransactionId(Long transactionId){
        return transactionItemRepository.findByTransactionId(transactionId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public TransactionItem getTransactionItemById(Long id){
        return transactionItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Item not found with id: " + id));
    }

    public TransactionItemResponseDTO getTransactionItemByIdDTO(Long id){return toDTO(getTransactionItemById(id));}

    public List<TransactionItemResponseDTO> getItemsByRetailerId(Long retailerId){
        return transactionItemRepository.findByTransaction_Retailer_Id(retailerId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public TransactionItemResponseDTO addTransactionItem(TransactionItem transactionItem){
        TransactionItem saved = transactionItemRepository.save(transactionItem);

        ProductVariants product = saved.getProductVariant();
        product.setCurrentStock(product.getCurrentStock() - saved.getQuantitySupplied());
        productVariantsRepository.save(product);
        return toDTO(saved);
    }

    public void deleteTransactionItem(Long id){
        transactionItemRepository.deleteById(id);
    }

}
