package com.fingold.config;

import com.fingold.entity.GoldPrice;
import com.fingold.entity.User;
import com.fingold.entity.Wallet;
import com.fingold.repository.GoldPriceRepository;
import com.fingold.repository.UserRepository;
import com.fingold.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Seeds the database with:
 *  - A default ADMIN user (change credentials in production!)
 *  - A starting demo gold price
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final GoldPriceRepository goldPriceRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedAdmin();
        seedInitialGoldPrice();
    }

    private void seedAdmin() {
        if (userRepository.existsByEmail("admin@fingold.com")) return;

        User admin = User.builder()
                .fullName("Super Admin")
                .email("admin@fingold.com")
                .phone("9000000000")
                .password(passwordEncoder.encode("Admin@1234"))
                .role(User.Role.ADMIN)
                .enabled(true)
                .emailVerified(true)
                .kycStatus("VERIFIED")
                .build();
        admin = userRepository.save(admin);

        walletRepository.save(Wallet.builder().user(admin).build());

        log.info("=== Default admin seeded: admin@fingold.com / Admin@1234 (CHANGE IN PRODUCTION) ===");
    }

    private void seedInitialGoldPrice() {
        if (goldPriceRepository.findByActiveTrue().isPresent()) return;

        GoldPrice price = GoldPrice.builder()
                .buyPricePerGram(new BigDecimal("6233.96"))
                .sellPricePerGram(new BigDecimal("6170.00"))
                .gstPercentage(new BigDecimal("3.0"))
                .validitySeconds(300)
                .source("SEED")
                .active(true)
                .build();
        goldPriceRepository.save(price);

        log.info("=== Initial gold price seeded: ₹6,233.96/gm (buy), ₹6,170.00/gm (sell) ===");
    }
}
