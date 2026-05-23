package com.fingold.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "gold_prices")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GoldPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Buy price per gram (exclusive of GST) */
    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal buyPricePerGram;

    /** Sell price per gram — typically slightly lower than buy */
    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal sellPricePerGram;

    /** GST rate in percent (e.g. 3.0) */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal gstPercentage;

    /** True = this is the currently active price */
    @Builder.Default
    @Column(nullable = false)
    private boolean active = false;

    /** When this price was set by an admin or the price scheduler */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /** How long (seconds) this price is valid */
    @Builder.Default
    @Column(nullable = false)
    private Integer validitySeconds = 300;

    /** Source label, e.g. "MMTC-PAMP", "MANUAL", "MOCK" */
    @Builder.Default
    @Column(length = 50)
    private String source = "MOCK";

    /** Admin who set the price (null if system-generated) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "set_by_user_id")
    private User setBy;
}
