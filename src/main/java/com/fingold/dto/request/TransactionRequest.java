package com.fingold.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

public class TransactionRequest {

    /** Used to create a BUY order */
    @Data
    public static class BuyRequest {

        /** Either amountInRupees OR goldGrams must be provided (not both) */
        @DecimalMin(value = "10.00", message = "Minimum purchase amount is ₹10")
        @DecimalMax(value = "200000.00", message = "Maximum purchase per transaction is ₹2,00,000")
        private BigDecimal amountInRupees;

        @DecimalMin(value = "0.001", message = "Minimum purchase is 0.001 grams")
        private BigDecimal goldGrams;

        @NotNull(message = "Gold price ID is required to lock the price")
        private Long goldPriceId;
    }

    /** Used to create a SELL order */
    @Data
    public static class SellRequest {

        @NotNull
        @DecimalMin(value = "0.001", message = "Minimum sell quantity is 0.001 grams")
        private BigDecimal goldGrams;

        @NotNull(message = "Gold price ID is required to lock the price")
        private Long goldPriceId;
    }

    /** Razorpay payment verification payload sent by frontend after payment */
    @Data
    public static class PaymentVerification {

        @NotBlank
        private String razorpayOrderId;

        @NotBlank
        private String razorpayPaymentId;

        @NotBlank
        private String razorpaySignature;

        @NotBlank
        private String orderReference;
    }
}
