package com.josalukkas.digigold.service.impl;

import com.josalukkas.digigold.dto.request.GoldPriceRequest;
import com.josalukkas.digigold.dto.response.ApiResponse;
import com.josalukkas.digigold.entity.GoldPrice;
import com.josalukkas.digigold.entity.User;
import com.josalukkas.digigold.exception.GlobalExceptionHandler.*;
import com.josalukkas.digigold.repository.GoldPriceRepository;
import com.josalukkas.digigold.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoldPriceService {

    private final GoldPriceRepository goldPriceRepository;
    private final UserRepository userRepository;

    @Value("${app.gold.gst-percentage}")
    private BigDecimal defaultGst;

    @Value("${app.gold.price-validity-seconds}")
    private int validitySeconds;

    private static final BigDecimal BASE_PRICE = new BigDecimal("6233.96");  // ₹/gram (demo)
    private final Random random = new Random();

    // ── Public: get live price ────────────────────────────────────

    @Transactional(readOnly = true)
    public ApiResponse.GoldPriceResponse getLivePrice() {
        GoldPrice price = goldPriceRepository.findByActiveTrue()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active gold price configured. Please contact admin."));
        return toResponse(price);
    }

    @Transactional(readOnly = true)
    public GoldPrice getActivePriceEntity() {
        return goldPriceRepository.findByActiveTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No active gold price."));
    }

    // ── Admin: set price manually ─────────────────────────────────

    @Transactional
    public ApiResponse.GoldPriceResponse setPrice(GoldPriceRequest req, String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        goldPriceRepository.deactivateAllPrices();

        GoldPrice price = GoldPrice.builder()
                .buyPricePerGram(req.getBuyPricePerGram())
                .sellPricePerGram(req.getSellPricePerGram())
                .gstPercentage(req.getGstPercentage())
                .validitySeconds(req.getValiditySeconds())
                .source(req.getSource())
                .active(true)
                .setBy(admin)
                .build();

        price = goldPriceRepository.save(price);
        log.info("Gold price updated by {} — buy: {}, sell: {}",
                adminEmail, price.getBuyPricePerGram(), price.getSellPricePerGram());
        return toResponse(price);
    }

    // ── Admin: price history ──────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ApiResponse.GoldPriceResponse> getPriceHistory(Pageable pageable) {
        return goldPriceRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toResponse);
    }

    // ── Scheduled: simulate live price every 5 minutes ───────────

    @Scheduled(fixedRateString = "#{${app.gold.price-validity-seconds} * 1000}")
    @Transactional
    public void simulateLivePriceUpdate() {
        // In production, replace this with a real gold API call (e.g. MMTC-PAMP feed)
        double fluctuation = (random.nextDouble() - 0.5) * 40; // ±₹20/gram swing
        BigDecimal buyPrice = BASE_PRICE
                .add(BigDecimal.valueOf(fluctuation))
                .setScale(2, RoundingMode.HALF_UP);

        // Sell price = buy price minus ~1% spread
        BigDecimal sellPrice = buyPrice
                .multiply(new BigDecimal("0.990"))
                .setScale(2, RoundingMode.HALF_UP);

        goldPriceRepository.deactivateAllPrices();

        GoldPrice price = GoldPrice.builder()
                .buyPricePerGram(buyPrice)
                .sellPricePerGram(sellPrice)
                .gstPercentage(defaultGst)
                .validitySeconds(validitySeconds)
                .source("MOCK")
                .active(true)
                .build();

        goldPriceRepository.save(price);
        log.debug("Mock price update: buy={}/gm, sell={}/gm", buyPrice, sellPrice);
    }

    // ── Mapping ───────────────────────────────────────────────────

    public ApiResponse.GoldPriceResponse toResponse(GoldPrice gp) {
        BigDecimal gstFactor = BigDecimal.ONE.add(
                gp.getGstPercentage().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));

        return ApiResponse.GoldPriceResponse.builder()
                .id(gp.getId())
                .buyPricePerGram(gp.getBuyPricePerGram())
                .sellPricePerGram(gp.getSellPricePerGram())
                .gstPercentage(gp.getGstPercentage())
                .active(gp.isActive())
                .validitySeconds(gp.getValiditySeconds())
                .source(gp.getSource())
                .createdAt(gp.getCreatedAt())
                .buyPriceWithGst(gp.getBuyPricePerGram().multiply(gstFactor)
                        .setScale(2, RoundingMode.HALF_UP))
                .sellPriceWithGst(gp.getSellPricePerGram().multiply(gstFactor)
                        .setScale(2, RoundingMode.HALF_UP))
                .build();
    }
}
