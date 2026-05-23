package com.fingold.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_tx_user", columnList = "user_id"),
        @Index(name = "idx_tx_status", columnList = "status"),
        @Index(name = "idx_tx_created", columnList = "createdAt")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-readable order reference e.g. DG-20240520-000123 */
    @Column(nullable = false, unique = true, length = 40)
    private String orderReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;   // BUY or SELL

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    /** Gold grams involved */
    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal goldGrams;

    /** Price per gram at time of transaction */
    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal pricePerGram;

    /** Base amount = goldGrams * pricePerGram */
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal baseAmount;

    /** GST amount */
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal taxAmount;

    /** Total amount paid / received */
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount;

    /** GST percentage applied */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal gstPercentage;

    /** Razorpay order id */
    @Column(length = 100)
    private String razorpayOrderId;

    /** Razorpay payment id (after successful payment) */
    @Column(length = 100)
    private String razorpayPaymentId;

    /** Razorpay signature for verification */
    @Column(length = 200)
    private String razorpaySignature;

    @Column(columnDefinition = "TEXT")
    private String failureReason;

    /** The gold price snapshot used for this transaction */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gold_price_id")
    private GoldPrice goldPrice;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    public enum TransactionType {
        BUY, SELL
    }

    public enum TransactionStatus {
        PENDING,        // order created, waiting for payment
        PAYMENT_INITIATED,
        COMPLETED,      // payment confirmed, gold credited/debited
        FAILED,         // payment failed or processing error
        CANCELLED,      // user cancelled before payment
        REFUND_INITIATED,
        REFUNDED
    }
}
