package com.josalukkas.digigold.dto.response;

import com.josalukkas.digigold.entity.Transaction;
import com.josalukkas.digigold.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ApiResponse {

    // ── Auth ──────────────────────────────────────────────────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AuthToken {
        private String accessToken;
        private String refreshToken;
        private String tokenType = "Bearer";
        private long expiresIn;      // seconds
        private UserProfile user;
    }

    // ── User ─────────────────────────────────────────────────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UserProfile {
        private Long id;
        private String fullName;
        private String email;
        private String phone;
        private User.Role role;
        private boolean emailVerified;
        private String kycStatus;
        private LocalDateTime createdAt;
    }

    // ── Gold Price ────────────────────────────────────────────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class GoldPriceResponse {
        private Long id;
        private BigDecimal buyPricePerGram;
        private BigDecimal sellPricePerGram;
        private BigDecimal gstPercentage;
        private boolean active;
        private Integer validitySeconds;
        private String source;
        private LocalDateTime createdAt;
        // Derived helpers for UI
        private BigDecimal buyPriceWithGst;   // buyPrice * (1 + gst/100)
        private BigDecimal sellPriceWithGst;
    }

    // ── Wallet ────────────────────────────────────────────────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class WalletResponse {
        private Long id;
        private BigDecimal goldBalanceGrams;
        private BigDecimal inrBalance;
        private BigDecimal totalGoldBought;
        private BigDecimal totalGoldSold;
        private LocalDateTime updatedAt;
        // Derived: current value in INR at live sell price
        private BigDecimal currentValueInr;
    }

    // ── Transaction ───────────────────────────────────────────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TransactionResponse {
        private Long id;
        private String orderReference;
        private Transaction.TransactionType type;
        private Transaction.TransactionStatus status;
        private BigDecimal goldGrams;
        private BigDecimal pricePerGram;
        private BigDecimal baseAmount;
        private BigDecimal taxAmount;
        private BigDecimal totalAmount;
        private BigDecimal gstPercentage;
        private String razorpayOrderId;
        private String razorpayPaymentId;
        private LocalDateTime createdAt;
        private LocalDateTime completedAt;
    }

    /** Returned after creating a BUY order — includes Razorpay order details */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BuyOrderCreated {
        private String orderReference;
        private String razorpayOrderId;
        private BigDecimal goldGrams;
        private BigDecimal pricePerGram;
        private BigDecimal baseAmount;
        private BigDecimal taxAmount;
        private BigDecimal totalAmount;   // amount in paise for Razorpay
        private long amountInPaise;
        private String currency;
        private String razorpayKeyId;     // frontend needs this to init Razorpay SDK
    }

    /** Returned after creating a SELL order */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SellOrderCreated {
        private String orderReference;
        private BigDecimal goldGrams;
        private BigDecimal pricePerGram;
        private BigDecimal baseAmount;
        private BigDecimal taxAmount;
        private BigDecimal netAmount;    // amount user receives
        private Transaction.TransactionStatus status;
    }

    // ── Admin Dashboard ───────────────────────────────────────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AdminStats {
        private long totalUsers;
        private long totalTransactions;
        private BigDecimal totalGoldSoldGrams;
        private BigDecimal totalRevenueInr;
        private GoldPriceResponse activePrices;
    }

    // ── Generic ───────────────────────────────────────────────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MessageResponse {
        private String message;
        private boolean success;
    }

    // ── Bank Account ──────────────────────────────────────────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BankAccountResponse {
        private Long id;
        private String accountNumber;
        private String ifscCode;
        private String accountHolderName;
        private String bankName;
        private boolean verified;
    }

}