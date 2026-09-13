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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

    @Transactional
    public TransactionItemResponseDTO addTransactionItem(TransactionItem transactionItem){

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

    // NEW: completes (fully or partially) a previously partial delivery.
    // additionalQuantity is added to the EXISTING quantitySupplied — this
    // method never touches quantityOrdered or re-decrements the amount
    // already supplied earlier. Stock only ever moves by the NEW amount
    // leaving today, same principle as addTransactionItem()'s original
    // stock decrement.
    @Transactional
    public TransactionItemResponseDTO supplyRemaining(Long id, SupplyRemainingRequest request) {
        TransactionItem item = getTransactionItemById(id);

        if (request.additionalQuantity() == null || request.additionalQuantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Additional quantity must be greater than zero.");
        }

        int newTotalSupplied = item.getQuantitySupplied() + request.additionalQuantity();

        // Can't supply more than was ever ordered — this isn't a NEW sale,
        // it's completing an existing one.
        if (newTotalSupplied > item.getQuantityOrdered()) {
            int stillOwed = item.getQuantityOrdered() - item.getQuantitySupplied();
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only " + stillOwed + " unit(s) are still owed on this item — cannot supply "
                            + request.additionalQuantity() + ".");
        }

        // Same stock-sufficiency guard as addTransactionItem(), checked
        // against ONLY the new amount, not the running total — the earlier
        // supplied portion already moved stock when it happened.
        if (item.getProductVariant() != null) {
            ProductVariants variant = item.getProductVariant();
            if (request.additionalQuantity() > variant.getCurrentStock()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Not enough stock for " + variant.getSize() + " " + variant.getProducer()
                                + " — only " + variant.getCurrentStock() + " available.");
            }
            variant.setCurrentStock(variant.getCurrentStock() - request.additionalQuantity());
            productVariantsRepository.save(variant);
        } else if (item.getProduct() != null) {
            Product product = item.getProduct();
            if (request.additionalQuantity() > product.getCurrentStock()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Not enough stock for " + product.getName()
                                + " — only " + product.getCurrentStock() + " available.");
            }
            product.setCurrentStock(product.getCurrentStock() - request.additionalQuantity());
            productRepository.save(product);
        } else {
            throw new RuntimeException("This item has no product reference.");
        }

        item.setQuantitySupplied(newTotalSupplied);
        item.setSupplyStatus(newTotalSupplied < item.getQuantityOrdered() ? "partially_supplied" : "supplied");

        // Preserve the original note rather than overwrite it — this could
        // be the second or third partial completion of the same item, and
        // losing "rest completed at Branch 2" the moment someone records a
        // follow-up delivery would erase real information.
        if (request.note() != null && !request.note().isBlank()) {
            item.setSupplyNote(
                    item.getSupplyNote() == null || item.getSupplyNote().isBlank()
                            ? request.note()
                            : item.getSupplyNote() + "; " + request.note()
            );
        }

        TransactionItem saved = transactionItemRepository.save(item);
        return toDTO(saved);
    }
}