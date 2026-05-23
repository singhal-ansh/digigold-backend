package com.fingold.repository;

import com.fingold.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Transaction> findByUserIdAndTypeOrderByCreatedAtDesc(
            Long userId, Transaction.TransactionType type, Pageable pageable);

    Optional<Transaction> findByOrderReference(String orderReference);
    Optional<Transaction> findByRazorpayOrderId(String razorpayOrderId);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.status = 'COMPLETED'")
    long countCompleted();

    @Query("SELECT COALESCE(SUM(t.totalAmount), 0) FROM Transaction t " +
           "WHERE t.type = 'BUY' AND t.status = 'COMPLETED'")
    BigDecimal sumCompletedBuyAmount();

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.user.id = :userId AND t.status = 'COMPLETED'")
    long countCompletedByUser(@Param("userId") Long userId);

    @Query("SELECT t FROM Transaction t WHERE t.createdAt BETWEEN :from AND :to")
    Page<Transaction> findByDateRange(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    /** For admin dashboard stats */
    @Query("SELECT COALESCE(SUM(t.goldGrams), 0) FROM Transaction t " +
           "WHERE t.type = 'BUY' AND t.status = 'COMPLETED'")
    BigDecimal sumGoldBoughtGrams();
}
