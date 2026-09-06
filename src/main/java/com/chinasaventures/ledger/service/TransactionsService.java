package com.chinasaventures.ledger.service;

import com.chinasaventures.ledger.dto.*;
import com.chinasaventures.ledger.model.*;
import com.chinasaventures.ledger.repository.*;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // NEW
import org.springframework.web.server.ResponseStatusException; // NEW

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionsService {
    private final TransactionsRepository transactionsRepository;
    private final TransactionItemRepository transactionItemRepository; // unused directly now — item creation goes through TransactionItemService instead, see below
    private final ProductRepository productRepository;
    private final ProductVariantsRepository productVariantsRepository; // NEW — needed to resolve variant prices
    private final UsersRepository usersRepository;
    private final RetailersRepository retailersRepository; // NEW — assumed plain JpaRepository, same as every other repo here
    private final TransactionItemService transactionItemService; // NEW — reuses the stock-safety-checked, supplyStatus-deriving logic from last message instead of duplicating it here

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

        return new TransactionResponseDTO(
                t.getId(), t.getCustomerName(), t.getCustomerPhone(),
                t.getTotalAmount(), t.getAmountPaid(), t.getPaymentStatus(),
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

    // CHANGED: entire method rewritten. Previously took a raw Transactions
    // object (with totalAmount already set by whoever called it — trusted,
    // unvalidated) and never touched items at all. Now takes
    // CreateTransactionRequest, resolves every item's real Product/Variant,
    // computes totalAmount server-side from actual prices, and creates the
    // items alongside the transaction — all as one atomic unit.
    @Transactional
    public TransactionResponseDTO addTransaction(CreateTransactionRequest request){

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String identifier = auth.getName();

        Users currentUser = usersRepository.findByEmailOrPhoneNumber(identifier, identifier)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found: " + identifier));

        // NEW: retailer is optional — null means walk-in, same convention
        // as everywhere else in this app.
        Retailers retailer = null;
        if (request.retailerId() != null) {
            retailer = retailersRepository.findById(request.retailerId())
                    .orElseThrow(() -> new RuntimeException("Retailer not found with id: " + request.retailerId()));
        }

        // NEW: resolve every line item's real Product/Variant up front, and
        // build the (unsaved, not-yet-linked-to-a-transaction) TransactionItem
        // entities here. Building them now — before totalAmount or the
        // Transactions row even exist — lets one pass compute both the running
        // total AND have the fully-populated items ready to save afterward,
        // instead of hitting the database twice for the same product/variant.
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
                unitPrice = variant.getPricePerUnit(); // variant price overrides product price when a variant is picked
            }

            // Billed on the FULL quantityOrdered, per your confirmed decision —
            // not quantitySupplied. A partial delivery is a logistics fact,
            // not a discount; the customer owes for the whole order.
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(itemRequest.quantityOrdered()));
            totalAmount = totalAmount.add(lineTotal);

            TransactionItem item = new TransactionItem();
            item.setProduct(product);
            item.setProductVariant(variant);
            item.setQuantityOrdered(itemRequest.quantityOrdered());
            item.setQuantitySupplied(itemRequest.quantitySupplied());
            item.setSupplyNote(itemRequest.supplyNote());
            // supplyStatus is NOT set here — addTransactionItem() derives it
            // server-side from quantityOrdered vs quantitySupplied, same as
            // paymentType below is derived rather than trusted from input.
            itemsToCreate.add(item);
        }

        // NEW: null-safe — request.amountPaid() could be omitted by the
        // frontend. The entity's own default (0) only applies if we never
        // call setAmountPaid at all, and here we're building the object by
        // hand, so this guard has to be explicit.
        BigDecimal amountPaid = request.amountPaid() != null ? request.amountPaid() : BigDecimal.ZERO;

        Transactions transaction = new Transactions();
        transaction.setCustomerName(request.customerName());
        transaction.setCustomerPhone(request.customerPhone());
        transaction.setTotalAmount(totalAmount); // CHANGED: computed, never trusted from the client
        transaction.setAmountPaid(amountPaid);
        transaction.setRetailer(retailer);
        transaction.setRecordedBy(currentUser);
        transaction.setBranch(currentUser.getBranch());
        transaction.setPaymentStatus("pending");

        // Unchanged paymentType logic — same three-way split as before, just
        // now running against a server-computed totalAmount instead of a
        // client-supplied one.
        BigDecimal debt = totalAmount.subtract(amountPaid);
        if (debt.compareTo(totalAmount) == 0) {
            transaction.setPaymentType("credit");
        } else if (debt.compareTo(BigDecimal.ZERO) > 0) {
            transaction.setPaymentType("part_payment");
        } else {
            transaction.setPaymentType("full");
        }

        Transactions savedTransaction = transactionsRepository.save(transaction);

        // NEW: link each item to the now-saved (id-bearing) transaction, then
        // create it through TransactionItemService — which runs the
        // stock-sufficiency check and decrements stock. Because this whole
        // method is @Transactional, if item 3 of 4 fails here (e.g. not
        // enough stock), Spring rolls back items 1-2 AND the Transactions
        // row above, not just item 3's own failed write.
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

        // NOT CHANGED — this is the tautological bug flagged above. Left
        // exactly as-is pending your decision on whether to fix it now.
        if ("confirmed".equalsIgnoreCase(transaction.getPaymentStatus())) {
            transaction.setPaymentStatus("confirmed");
            transaction.setConfirmedBy(currentUser);
            transaction.setConfirmedAt(LocalDateTime.now());
        } else {
            transaction.setPaymentStatus("pending");
        }
        transaction.setConfirmedAt(LocalDateTime.now());
        transaction.setConfirmedBy(currentUser);
        transaction.setPaymentProof(paymentProof);

        Transactions saved = transactionsRepository.save(transaction);
        return toDTO(saved);
    }

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

        transactionsRepository.deleteById(id);
    }
}