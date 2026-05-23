package com.fingold.service.impl;

import com.fingold.config.RazorpayConfig;
import com.fingold.dto.request.TransactionRequest;
import com.fingold.dto.response.ApiResponse;
import com.fingold.entity.*;
import com.fingold.exception.GlobalExceptionHandler.*;
import com.fingold.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final GoldPriceRepository goldPriceRepository;
    private final UserRepository userRepository;
    private final RazorpayService razorpayService;
    private final RazorpayConfig razorpayConfig;

    @Value("${app.gold.min-buy-amount}")
    private BigDecimal minBuyAmount;

    @Value("${app.gold.max-buy-amount}")
    private BigDecimal maxBuyAmount;

    @Value("${app.gold.min-sell-grams}")
    private BigDecimal minSellGrams;

    private static final DateTimeFormatter REF_DATE_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    // ── BUY flow ─────────────────────────────────────────────────

    /**
     * Step 1 of buy: validate, lock price, create Razorpay order, persist PENDING transaction.
     * The user then completes payment on the frontend and calls verifyPayment().
     */
    @Transactional
    public ApiResponse.BuyOrderCreated initiateBuy(TransactionRequest.BuyRequest req,
                                                   String userEmail) {
        User user = getUser(userEmail);

        // Validate gold price is still active
        GoldPrice price = getAndValidatePrice(req.getGoldPriceId());

        // Derive gold grams or rupee amount
        BigDecimal goldGrams;
        BigDecimal baseAmount;

        if (req.getAmountInRupees() != null) {
            baseAmount = req.getAmountInRupees();
            goldGrams = baseAmount
                    .divide(price.getBuyPricePerGram(), 6, RoundingMode.HALF_DOWN);
        } else if (req.getGoldGrams() != null) {
            goldGrams = req.getGoldGrams().setScale(6, RoundingMode.HALF_DOWN);
            baseAmount = goldGrams.multiply(price.getBuyPricePerGram())
                    .setScale(2, RoundingMode.HALF_UP);
        } else {
            throw new IllegalArgumentException("Provide either amountInRupees or goldGrams");
        }

        if (baseAmount.compareTo(minBuyAmount) < 0) {
            throw new IllegalArgumentException("Minimum purchase is ₹" + minBuyAmount);
        }
        if (baseAmount.compareTo(maxBuyAmount) > 0) {
            throw new IllegalArgumentException("Maximum purchase per transaction is ₹" + maxBuyAmount);
        }

        // Tax
        BigDecimal taxAmount = baseAmount
                .multiply(price.getGstPercentage())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = baseAmount.add(taxAmount);

        String orderReference = generateOrderRef();

        // Create Razorpay order
        Map<String, Object> rzpOrder = razorpayService.createOrder(totalAmount, orderReference);
        String rzpOrderId = (String) rzpOrder.get("id");

        // Persist PENDING transaction
        Transaction tx = Transaction.builder()
                .orderReference(orderReference)
                .user(user)
                .type(Transaction.TransactionType.BUY)
                .status(Transaction.TransactionStatus.PENDING)
                .goldGrams(goldGrams)
                .pricePerGram(price.getBuyPricePerGram())
                .baseAmount(baseAmount)
                .taxAmount(taxAmount)
                .totalAmount(totalAmount)
                .gstPercentage(price.getGstPercentage())
                .razorpayOrderId(rzpOrderId)
                .goldPrice(price)
                .build();
        transactionRepository.save(tx);

        long amountInPaise = ((Number) rzpOrder.get("amount")).longValue();

        return ApiResponse.BuyOrderCreated.builder()
                .orderReference(orderReference)
                .razorpayOrderId(rzpOrderId)
                .goldGrams(goldGrams)
                .pricePerGram(price.getBuyPricePerGram())
                .baseAmount(baseAmount)
                .taxAmount(taxAmount)
                .totalAmount(totalAmount)
                .amountInPaise(amountInPaise)
                .currency(razorpayConfig.getCurrency())
                .razorpayKeyId(razorpayConfig.getKeyId())
                .build();
    }

    /**
     * Step 2 of buy: verify Razorpay payment, credit gold to wallet.
     */
    @Transactional
    public ApiResponse.TransactionResponse verifyPayment(
            TransactionRequest.PaymentVerification req, String userEmail) {

        Transaction tx = transactionRepository.findByOrderReference(req.getOrderReference())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found: " + req.getOrderReference()));

        if (!tx.getUser().getEmail().equals(userEmail)) {
            throw new ResourceNotFoundException("Order not found");
        }

        if (tx.getStatus() != Transaction.TransactionStatus.PENDING) {
            throw new IllegalStateException("Transaction already processed: " + tx.getStatus());
        }

        // Verify Razorpay signature
        razorpayService.verifyPaymentSignature(
                req.getRazorpayOrderId(), req.getRazorpayPaymentId(), req.getRazorpaySignature());

        // Update transaction
        tx.setStatus(Transaction.TransactionStatus.COMPLETED);
        tx.setRazorpayPaymentId(req.getRazorpayPaymentId());
        tx.setRazorpaySignature(req.getRazorpaySignature());
        tx.setCompletedAt(LocalDateTime.now());
        transactionRepository.save(tx);

        // Credit gold to wallet (pessimistic lock)
        Wallet wallet = walletRepository.findByUserIdForUpdate(tx.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        wallet.setGoldBalanceGrams(wallet.getGoldBalanceGrams().add(tx.getGoldGrams()));
        wallet.setTotalGoldBought(wallet.getTotalGoldBought().add(tx.getGoldGrams()));
        walletRepository.save(wallet);

        log.info("BUY completed: {} — {}g gold credited to user {}",
                tx.getOrderReference(), tx.getGoldGrams(), userEmail);

        return toResponse(tx);
    }

    // ── SELL flow ─────────────────────────────────────────────────

    /**
     * Sell is atomic (no third-party payment): debit gold, credit INR instantly.
     */
    @Transactional
    public ApiResponse.SellOrderCreated initiateSell(TransactionRequest.SellRequest req,
                                                     String userEmail) {
        User user = getUser(userEmail);

        if (req.getGoldGrams().compareTo(minSellGrams) < 0) {
            throw new IllegalArgumentException("Minimum sell quantity is " + minSellGrams + " grams");
        }

        GoldPrice price = getAndValidatePrice(req.getGoldPriceId());

        // Check wallet balance (pessimistic lock)
        Wallet wallet = walletRepository.findByUserIdForUpdate(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        if (wallet.getGoldBalanceGrams().compareTo(req.getGoldGrams()) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient gold balance. Available: " + wallet.getGoldBalanceGrams() + "g");
        }

        BigDecimal goldGrams = req.getGoldGrams().setScale(6, RoundingMode.HALF_DOWN);
        BigDecimal baseAmount = goldGrams.multiply(price.getSellPricePerGram())
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxAmount = baseAmount
                .multiply(price.getGstPercentage())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal netAmount = baseAmount.subtract(taxAmount);

        String orderReference = generateOrderRef();

        // Debit wallet
        wallet.setGoldBalanceGrams(wallet.getGoldBalanceGrams().subtract(goldGrams));
        wallet.setTotalGoldSold(wallet.getTotalGoldSold().add(goldGrams));
        wallet.setInrBalance(wallet.getInrBalance().add(netAmount));
        walletRepository.save(wallet);

        // Persist COMPLETED transaction immediately (sell doesn't need payment gateway)
        Transaction tx = Transaction.builder()
                .orderReference(orderReference)
                .user(user)
                .type(Transaction.TransactionType.SELL)
                .status(Transaction.TransactionStatus.COMPLETED)
                .goldGrams(goldGrams)
                .pricePerGram(price.getSellPricePerGram())
                .baseAmount(baseAmount)
                .taxAmount(taxAmount)
                .totalAmount(netAmount)
                .gstPercentage(price.getGstPercentage())
                .goldPrice(price)
                .completedAt(LocalDateTime.now())
                .build();
        transactionRepository.save(tx);

        log.info("SELL completed: {} — {}g gold sold by {}", orderReference, goldGrams, userEmail);

        return ApiResponse.SellOrderCreated.builder()
                .orderReference(orderReference)
                .goldGrams(goldGrams)
                .pricePerGram(price.getSellPricePerGram())
                .baseAmount(baseAmount)
                .taxAmount(taxAmount)
                .netAmount(netAmount)
                .status(Transaction.TransactionStatus.COMPLETED)
                .build();
    }

    // ── Transaction history ───────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ApiResponse.TransactionResponse> getUserTransactions(String userEmail,
                                                                     Pageable pageable) {
        User user = getUser(userEmail);
        return transactionRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ApiResponse.TransactionResponse getTransactionDetail(String orderRef, String userEmail) {
        Transaction tx = transactionRepository.findByOrderReference(orderRef)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderRef));
        if (!tx.getUser().getEmail().equals(userEmail)) {
            throw new ResourceNotFoundException("Order not found");
        }
        return toResponse(tx);
    }

    // ── Admin: all transactions ───────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ApiResponse.TransactionResponse> getAllTransactions(Pageable pageable) {
        return transactionRepository.findAll(pageable).map(this::toResponse);
    }

    // ── Helpers ──────────────────────────────────────────────────

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private GoldPrice getAndValidatePrice(Long priceId) {
        GoldPrice price = goldPriceRepository.findById(priceId)
                .orElseThrow(() -> new ResourceNotFoundException("Gold price not found: " + priceId));
        if (!price.isActive()) {
            throw new PriceExpiredException(
                    "The quoted price has expired. Please fetch the latest price and retry.");
        }
        return price;
    }

    private String generateOrderRef() {
        String date = LocalDateTime.now().format(REF_DATE_FMT);
        String uid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return String.format("DG-%s-%s", date, uid);
    }

    public ApiResponse.TransactionResponse toResponse(Transaction tx) {
        return ApiResponse.TransactionResponse.builder()
                .id(tx.getId())
                .orderReference(tx.getOrderReference())
                .type(tx.getType())
                .status(tx.getStatus())
                .goldGrams(tx.getGoldGrams())
                .pricePerGram(tx.getPricePerGram())
                .baseAmount(tx.getBaseAmount())
                .taxAmount(tx.getTaxAmount())
                .totalAmount(tx.getTotalAmount())
                .gstPercentage(tx.getGstPercentage())
                .razorpayOrderId(tx.getRazorpayOrderId())
                .razorpayPaymentId(tx.getRazorpayPaymentId())
                .createdAt(tx.getCreatedAt())
                .completedAt(tx.getCompletedAt())
                .build();
    }
}