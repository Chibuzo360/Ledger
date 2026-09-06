package com.chinasaventures.ledger.service;

import com.chinasaventures.ledger.dto.*;
import com.chinasaventures.ledger.model.ProductVariants;
import com.chinasaventures.ledger.repository.ProductVariantsRepository;
import com.chinasaventures.ledger.repository.TransactionItemRepository;
import com.chinasaventures.ledger.model.TransactionItem;
import com.chinasaventures.ledger.model.ProductCategory;
import com.chinasaventures.ledger.model.Product;
import com.chinasaventures.ledger.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // NEW
import org.springframework.web.server.ResponseStatusException; // NEW

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionItemService {
    private final TransactionItemRepository transactionItemRepository;
    private final ProductVariantsRepository productVariantsRepository;
    private final ProductRepository productRepository;

    private ProductCategory pCategory(TransactionItem item){
        if (item != null && item.getProduct() != null && item.getProduct().getCategory() != null) {
            return item.getProduct().getCategory();
        }
        return null;
    }

    private TransactionItemResponseDTO toDTO(TransactionItem txnItem){
        RetailerSummaryDTO retailer = txnItem.getTransaction().getRetailer() != null
                ? new RetailerSummaryDTO(
                txnItem.getTransaction().getRetailer().getId(),
                txnItem.getTransaction().getRetailer().getBusinessName(),
                txnItem.getTransaction().getRetailer().getContactName()
        ) : null;

        ProductCategoryDTO productCategory = pCategory(txnItem) != null
                ? new ProductCategoryDTO(pCategory(txnItem).getId(), pCategory(txnItem).getName())
                : null;

        TransactionSummaryDTO transaction = txnItem.getTransaction() != null
                ? new TransactionSummaryDTO(
                txnItem.getTransaction().getId(),
                txnItem.getTransaction().getCustomerName(),
                txnItem.getTransaction().getTotalAmount(),
                txnItem.getTransaction().getAmountPaid(),
                retailer)
                : null;

        ProductSummaryDTO product = txnItem.getProduct() != null
                ? new ProductSummaryDTO(
                txnItem.getProduct().getId(),
                txnItem.getProduct().getName(),
                txnItem.getProduct().getPricePerUnit(),
                productCategory,
                txnItem.getProduct().getCurrentStock()
        ) : null;

        ProductVariantSummaryDTO productVariant = txnItem.getProductVariant() != null
                ? new ProductVariantSummaryDTO(
                txnItem.getProductVariant().getId(),
                product,
                txnItem.getProductVariant().getPricePerUnit(),
                txnItem.getProductVariant().getSize(),
                txnItem.getProductVariant().getProducer(),
                txnItem.getProductVariant().getCurrentStock()
        ) : null;

        // CHANGED: supplyNote appended — remember to add the matching
        // component to TransactionItemResponseDTO (record) and this
        // constructor call has to stay positionally in sync with it.
        return new TransactionItemResponseDTO(
                txnItem.getId(), transaction, product, productVariant,
                txnItem.getQuantityOrdered(), txnItem.getQuantitySupplied(),
                txnItem.getSupplyStatus(), txnItem.getSupplyNote()
        );
    }

    public List<TransactionItemResponseDTO> getAllTransactionItems(){
        return transactionItemRepository.findAll().stream().map(this::toDTO).toList();
    }

    public List<TransactionItemResponseDTO> getItemsByTransactionId(Long transactionId){
        return transactionItemRepository.findByTransactionId(transactionId).stream().map(this::toDTO).toList();
    }

    public TransactionItem getTransactionItemById(Long id){
        return transactionItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Item not found with id: " + id));
    }

    public TransactionItemResponseDTO getTransactionItemByIdDTO(Long id){ return toDTO(getTransactionItemById(id)); }

    public List<TransactionItemResponseDTO> getItemsByRetailerId(Long retailerId){
        return transactionItemRepository.findByTransaction_Retailer_Id(retailerId).stream().map(this::toDTO).toList();
    }

    // CHANGED: @Transactional added. Two writes happen here (the item
    // itself, then the stock decrement) — without this, a crash between
    // the two leaves a TransactionItem on record that claims stock moved
    // when it never actually did. This matters far more now that this
    // method gets called once per line item inside a multi-item sale —
    // one failure needs to roll back everything in that sale, not just
    // this one item's half-finished write.
    @Transactional
    public TransactionItemResponseDTO addTransactionItem(TransactionItem transactionItem){

        // NEW: stock-sufficiency guard, checked BEFORE any write happens.
        // Deliberately compares against quantitySupplied, not
        // quantityOrdered — supplying less than ordered (with the gap
        // explained via supplyNote) is a valid, expected case; this only
        // catches someone trying to physically remove more than exists.
        if (transactionItem.getProductVariant() != null) {
            ProductVariants variant = transactionItem.getProductVariant();
            if (transactionItem.getQuantitySupplied() > variant.getCurrentStock()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Not enough stock for " + variant.getSize() + " " + variant.getProducer()
                                + " — only " + variant.getCurrentStock() + " available.");
            }
        } else if (transactionItem.getProduct() != null) {
            Product product = transactionItem.getProduct();
            if (transactionItem.getQuantitySupplied() > product.getCurrentStock()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Not enough stock for " + product.getName()
                                + " — only " + product.getCurrentStock() + " available.");
            }
        } else {
            throw new RuntimeException("This item has no product reference.");
        }

        // NEW: supplyStatus derived here, never trusted from the caller —
        // same principle as paymentType on Transactions being computed
        // server-side instead of accepted as input.
        int ordered = transactionItem.getQuantityOrdered();
        int supplied = transactionItem.getQuantitySupplied();
        if (supplied <= 0) {
            transactionItem.setSupplyStatus("not_supplied");
        } else if (supplied < ordered) {
            transactionItem.setSupplyStatus("partially_supplied");
        } else {
            transactionItem.setSupplyStatus("supplied");
        }

        TransactionItem saved = transactionItemRepository.save(transactionItem);

        if (saved.getProductVariant() != null) {
            ProductVariants productVariant = saved.getProductVariant();
            productVariant.setCurrentStock(productVariant.getCurrentStock() - saved.getQuantitySupplied());
            productVariantsRepository.save(productVariant);
        } else {
            Product product = saved.getProduct();
            product.setCurrentStock(product.getCurrentStock() - saved.getQuantitySupplied());
            productRepository.save(product);
        }

        return toDTO(saved);
    }

    public void deleteTransactionItem(Long id){
        transactionItemRepository.deleteById(id);
    }
}