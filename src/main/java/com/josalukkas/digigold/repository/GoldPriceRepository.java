package com.josalukkas.digigold.repository;

import com.josalukkas.digigold.entity.GoldPrice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface GoldPriceRepository extends JpaRepository<GoldPrice, Long> {

    Optional<GoldPrice> findByActiveTrue();

    @Modifying
    @Query("UPDATE GoldPrice gp SET gp.active = false WHERE gp.active = true")
    void deactivateAllPrices();

    Page<GoldPrice> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
