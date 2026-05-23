package com.fingold.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallets")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /** Gold balance in grams (up to 6 decimal places = micrograms) */
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal goldBalanceGrams = BigDecimal.ZERO;

    /** INR balance (for demo purposes; represents linked bank/UPI) */
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal inrBalance = BigDecimal.ZERO;

    /** Total gold ever bought (grams) - cumulative metric */
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal totalGoldBought = BigDecimal.ZERO;

    /** Total gold ever sold (grams) - cumulative metric */
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal totalGoldSold = BigDecimal.ZERO;

    @Version
    private Long version;   // optimistic locking to prevent race conditions

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
