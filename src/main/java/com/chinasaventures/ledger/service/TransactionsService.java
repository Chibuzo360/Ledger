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

    // NEW: the only payment methods a clerk directly witnesses succeed or
    // fail in the moment -- cash counted by hand, or a POS terminal showing
    // its own result. BUSINESS_ACCOUNT (transfer to the director) and
    // MIXED are deliberately excluded from this list everywhere it's used
    // below -- neither can be confirmed by anyone but the director.
    private static final List<String> WITNESSED_METHODS = List.of("CASH", "POS_TRANSFER", "POS_CARD");
    private static final List<String> VALID_PAYMENT_METHODS =
            List.of("CASH", "POS_TRANSFER", "POS_CARD", "BUSINESS_ACCOUNT", "MIXED");

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

        // CHANGED: paymentMethod/paymentMethodNote appended at the end,
        // matching the DTO's own field order exactly -- both are tail
        // fields on the record, so this can't repeat the earlier
        // amountPaid/discountAmount position swap.
        return new TransactionResponseDTO(
                t.getId(), t.getCustomerName(), t.getCustomerPhone(),
                t.getTotalAmount(), t.getDiscountAmount(), t.getAmountPaid(), t.getPaymentStatus(),
                t.getPaymentType(), t.getPaymentProof(),
                recordedBy, confirmedBy, retailer, t.getConfirmedAt(), t.getCreatedAt(),
                t.getPaymentMethod(), t.getPaymentMethodNote()
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

        // NEW: payment method validation, up front, before any items are
        // resolved -- fail fast on bad input rather than doing partial work
        // first.
        String paymentMethod = request.paymentMethod();
        if (paymentMethod == null || !VALID_PAYMENT_METHODS.contains(paymentMethod)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Payment method is required and must be one of: " + VALID_PAYMENT_METHODS);
        }
        if ("MIXED".equals(paymentMethod)
                && (request.paymentMethodNote() == null || request.paymentMethodNote().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A breakdown note is required when payment method is Mixed.");
        }

        // NEW: quantity validation, up front, before any item is resolved.
        // Without this a negative quantitySupplied slipped past the stock
        // check (-5 > stock is false) and INCREASED stock, a negative
        // quantityOrdered produced a negative line total, and an empty
        // items list created a sale with nothing in it. The frontend
        // blocks all of these, but hiding things in the UI is never
        // treated as real authorization in this codebase.
        if (request.items() == null || request.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A sale must contain at least one item.");
        }
        for (TransactionItemRequest itemRequest : request.items()) {
            Integer ordered = itemRequest.quantityOrdered();
            Integer supplied = itemRequest.quantitySupplied();
            if (itemRequest.productId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Every item must reference a product.");
            }
            if (ordered == null || ordered <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Quantity ordered must be greater than zero.");
            }
            if (supplied == null || supplied < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Quantity supplied cannot be negative.");
            }
            if (supplied > ordered) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Quantity supplied cannot exceed quantity ordered.");
            }
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
                    "Discount of N" + discountAmount + " exceeds the approved cap of N" + maxAllowedDiscount + ".");
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
        transaction.setPaymentMethod(paymentMethod); // NEW
        transaction.setPaymentMethodNote(request.paymentMethodNote()); // NEW
        transaction.setPaymentStatus("pending"); // default, may be overridden below

        BigDecimal debt = totalAmount.subtract(amountPaid);
        if (debt.compareTo(totalAmount) == 0) {
            transaction.setPaymentType("credit");
        } else if (debt.compareTo(BigDecimal.ZERO) > 0) {
            transaction.setPaymentType("part_payment");
        } else {
            transaction.setPaymentType("full");
        }

        // NEW: a witnessed method (cash/POS) can be marked confirmed right
        // at creation, because the clerk directly saw it succeed -- no
        // director verification needed for those. BUSINESS_ACCOUNT and
        // MIXED can NEVER be confirmed here regardless of what the client
        // sends -- this is enforced server-side, not just hidden in the UI,
        // matching this codebase's existing rule that frontend button-
        // hiding is never treated as real authorization.
        if (WITNESSED_METHODS.contains(paymentMethod) && Boolean.TRUE.equals(request.confirmedAtCreation())) {
            transaction.setPaymentStatus("confirmed");
            transaction.setConfirmedBy(currentUser);
            transaction.setConfirmedAt(LocalDateTime.now());
        }

        Transactions savedTransaction = transactionsRepository.save(transaction);

        for (TransactionItem item : itemsToCreate) {
            item.setTransaction(savedTransaction);
            transactionItemService.addTransactionItem(item);
        }

        return toDTO(savedTransaction);
    }

    // CHANGED: this method previously had NO role check at all -- any
    // logged-in user could confirm any transaction, regardless of who
    // recorded it or how it was paid. That gap is closed here: a director
    // can always confirm; a worker can only confirm when BOTH (a) the
    // director has switched on allowWorkerConfirmation, AND (b) the
    // transaction's payment method is one the clerk actually witnessed.
    // BUSINESS_ACCOUNT and MIXED transactions can never be confirmed by a
    // worker under any settings combination -- only a director can verify
    // those, by definition of what those methods mean.
    public TransactionResponseDTO confirmPayment(Long id, BigDecimal amountPaid, String paymentProof) {
        Transactions transaction = getTransactionById(id);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String identifier = auth.getName();
        Users currentUser = usersRepository.findByEmailOrPhoneNumber(identifier, identifier)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found: " + identifier));

        boolean isDirector = "director".equals(currentUser.getRole());
        if (!isDirector) {
            boolean workerConfirmationAllowed =
                    Boolean.TRUE.equals(discountSettingsService.getOrCreateSettings().getAllowWorkerConfirmation());
            boolean methodWasWitnessed = WITNESSED_METHODS.contains(transaction.getPaymentMethod());

            if (!workerConfirmationAllowed || !methodWasWitnessed) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Only a director can confirm this payment.");
            }
        }

        // NEW: once a transaction is BOTH confirmed AND paid in full, it's
        // genuinely settled — no further edits, matching this codebase's
        // "immutable once done" philosophy (StockIn, Transactions
        // deletion). Checked against the transaction's CURRENT stored
        // state, before any of this call's own changes are applied.
        if ("confirmed".equals(transaction.getPaymentStatus())
                && transaction.getAmountPaid().compareTo(transaction.getTotalAmount()) >= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This transaction is fully paid and confirmed — no further changes are allowed.");
        }

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

    // CHANGED: previously tiered by status -- a worker could delete their
    // own unconfirmed transactions outright. That capability is now
    // removed entirely per the settled decision: delete is director-only,
    // full stop, regardless of paymentStatus.
    //
    // CHANGED (stock restore): deleting a sale now means "this never
    // happened", so the stock its items decremented is put back BEFORE the
    // rows are removed. Without this, the "delete and re-enter to correct a
    // mistake" rule decremented stock twice (same bug StockIn had).
    // - Restores quantitySupplied, NOT quantityOrdered: stock only ever
    //   moved by what actually left. quantitySupplied is cumulative, so it
    //   already includes anything added later via supplyRemaining().
    // - Variant stock if the item has a variant, else product stock --
    //   mirroring exactly where addTransactionItem() took it from.
    // - All inside this one @Transactional: if any restore fails, nothing
    //   is restored AND nothing is deleted.
    @Transactional
    public void deleteTransaction(Long id){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String identifier = auth.getName();
        Users currentUser = usersRepository.findByEmailOrPhoneNumber(identifier, identifier)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found: " + identifier));

        if (!"director".equals(currentUser.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only a director can delete a transaction.");
        }

        // CHANGED: fail clearly on an unknown id instead of silently
        // no-oping (deleteById does nothing for a missing row).
        if (!transactionsRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Transaction not found with id: " + id);
        }

        List<TransactionItem> items = transactionItemRepository.findByTransactionId(id);
        for (TransactionItem item : items) {
            Integer supplied = item.getQuantitySupplied();
            if (supplied == null || supplied <= 0) {
                continue; // nothing left the shop for this line, nothing to put back
            }

            if (item.getProductVariant() != null) {
                ProductVariants variant = item.getProductVariant();
                int current = variant.getCurrentStock() != null ? variant.getCurrentStock() : 0;
                variant.setCurrentStock(current + supplied);
                productVariantsRepository.save(variant);
            } else if (item.getProduct() != null) {
                Product product = item.getProduct();
                int current = product.getCurrentStock() != null ? product.getCurrentStock() : 0;
                product.setCurrentStock(current + supplied);
                productRepository.save(product);
            }
        }

        transactionItemRepository.deleteByTransactionId(id);
        transactionsRepository.deleteById(id);
    }
}