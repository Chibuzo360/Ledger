package com.chinasaventures.ledger.service;

import com.chinasaventures.ledger.dto.*;
import com.chinasaventures.ledger.model.*;
import com.chinasaventures.ledger.repository.*;
import com.chinasaventures.ledger.service.DiscountSettingsService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionsService {
    private final TransactionsRepository transactionsRepository;
    private final TransactionItemRepository transactionItemRepository;
    private final ProductRepository productRepository;
    private final ProductVariantsRepository productVariantsRepository;
    private final UsersRepository usersRepository;
    private final RetailersRepository retailersRepository;
    private final TransactionItemService transactionItemService;
    private final DiscountSettingsService discountSettingsService;


    private TransactionResponseDTO toDTO(Transactions t) {
        UserSummaryDTO recordedBy = t.getRecordedBy() != null
                ? new UserSummaryDTO(t.getRecordedBy().getId(), t.getRecordedBy().getName(), t.getRecordedBy().getRole())
                : null;
        UserSummaryDTO confirmedBy = t.getConfirmedBy() != null
                ? new UserSummaryDTO(t.getConfirmedBy().getId(), t.getConfirmedBy().getName(), t.getConfirmedBy().getRole())
                : null;
        RetailerSummaryDTO retailer = t.getRetailer() != null
                ? new RetailerSummaryDTO(t.getRetailer().getId(), t.getRetailer().getBusinessName(), t.getCustomerName())
                : null;

        // CHANGED: I swapped amountPaid and DiscountAmount here — both
        // BigDecimal, so it compiled fine and silently corrupted every
        // transaction's displayed amountPaid/discountAmount. Fixed to match
        // the DTO's actual declared order (discountAmount before amountPaid).
        return new TransactionResponseDTO(
                t.getId(), t.getCustomerName(), t.getCustomerPhone(),
                t.getTotalAmount(), t.getDiscountAmount(), t.getAmountPaid(), t.getPaymentStatus(),
                t.getPaymentType(), t.getPaymentProof(),
                recordedBy, confirmedBy, retailer, t.getConfirmedAt(), t.getCreatedAt()
        );
    }

    public List<TransactionResponseDTO> getAllTransactions() {
        return transactionsRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public Transactions getTransactionById(Long id){
        return transactionsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found with id: "+ id));
    }

    public TransactionResponseDTO getTransactionByIdDTO(Long id) {
        return toDTO(getTransactionById(id));
    }

    @Transactional
    public TransactionResponseDTO addTransaction(CreateTransactionRequest request){

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String identifier = auth.getName();

        Users currentUser = usersRepository.findByEmailOrPhoneNumber(identifier, identifier)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found: " + identifier));

        Retailers retailer = null;
        if (request.retailerId() != null) {
            retailer = retailersRepository.findById(request.retailerId())
                    .orElseThrow(() -> new RuntimeException("Retailer not found with id: " + request.retailerId()));
        }

        List<TransactionItem> itemsToCreate = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (TransactionItemRequest itemRequest : request.items()) {
            Product product = productRepository.findById(itemRequest.productId())
                    .orElseThrow(() -> new RuntimeException("Product not found with id: " + itemRequest.productId()));

            ProductVariants variant = null;
            BigDecimal unitPrice = product.getPricePerUnit();

            if (itemRequest.productVariantId() != null) {
                variant = productVariantsRepository.findById(itemRequest.productVariantId())
                        .orElseThrow(() -> new RuntimeException("Variant not found with id: " + itemRequest.productVariantId()));
                unitPrice = variant.getPricePerUnit();
            }

            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(itemRequest.quantityOrdered()));
            totalAmount = totalAmount.add(lineTotal);

            TransactionItem item = new TransactionItem();
            item.setProduct(product);
            item.setProductVariant(variant);
            item.setQuantityOrdered(itemRequest.quantityOrdered());
            item.setQuantitySupplied(itemRequest.quantitySupplied());
            item.setSupplyNote(itemRequest.supplyNote());
            itemsToCreate.add(item);
        }

        BigDecimal subtotal = totalAmount;
        BigDecimal amountPaid = request.amountPaid() != null ? request.amountPaid() : BigDecimal.ZERO;

        BigDecimal discountAmount = request.discountAmount() != null ? request.discountAmount() : BigDecimal.ZERO;
        if (discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Discount cannot be negative.");
        }

        BigDecimal maxAllowedDiscount = discountSettingsService.getOrCreateSettings().getMaxDiscountAmount();
        if (discountAmount.compareTo(maxAllowedDiscount) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Discount of ₦" + discountAmount + " exceeds the approved cap of ₦" + maxAllowedDiscount + ".");
        }
        if (discountAmount.compareTo(subtotal) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Discount cannot exceed the sale's subtotal.");
        }

        totalAmount = subtotal.subtract(discountAmount);

        Transactions transaction = new Transactions();
        transaction.setCustomerName(request.customerName());
        transaction.setCustomerPhone(request.customerPhone());
        transaction.setTotalAmount(totalAmount);
        transaction.setAmountPaid(amountPaid);
        transaction.setDiscountAmount(discountAmount);
        transaction.setRetailer(retailer);
        transaction.setRecordedBy(currentUser);
        transaction.setBranch(currentUser.getBranch());
        transaction.setPaymentStatus("pending");

        BigDecimal debt = totalAmount.subtract(amountPaid);
        if (debt.compareTo(totalAmount) == 0) {
            transaction.setPaymentType("credit");
        } else if (debt.compareTo(BigDecimal.ZERO) > 0) {
            transaction.setPaymentType("part_payment");
        } else {
            transaction.setPaymentType("full");
        }

        Transactions savedTransaction = transactionsRepository.save(transaction);

        for (TransactionItem item : itemsToCreate) {
            item.setTransaction(savedTransaction);
            transactionItemService.addTransactionItem(item);
        }

        return toDTO(savedTransaction);
    }

    public TransactionResponseDTO confirmPayment(Long id, BigDecimal amountPaid, String paymentProof) {
        Transactions transaction = getTransactionById(id);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String identifier = auth.getName();
        Users currentUser = usersRepository.findByEmailOrPhoneNumber(identifier, identifier)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found: " + identifier));

        if (amountPaid != null) {
            if (amountPaid.compareTo(transaction.getTotalAmount()) > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Amount paid cannot exceed total amount owed");
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

        transaction.setPaymentStatus("confirmed");
        transaction.setConfirmedBy(currentUser);
        transaction.setConfirmedAt(LocalDateTime.now());
        transaction.setPaymentProof(paymentProof);

        Transactions saved = transactionsRepository.save(transaction);
        return toDTO(saved);
    }

    @Transactional
    public void deleteTransaction(Long id){
        Transactions transaction = getTransactionById(id);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String identifier = auth.getName();
        Users currentUser = usersRepository.findByEmailOrPhoneNumber(identifier, identifier)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found: " + identifier));

        if ("confirmed".equals(transaction.getPaymentStatus()) && !"director".equals(currentUser.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only a director can delete a confirmed transaction");
        }
        transactionItemRepository.deleteByTransactionId(id);
        transactionsRepository.deleteById(id);
    }
}