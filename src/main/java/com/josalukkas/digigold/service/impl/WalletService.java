package com.josalukkas.digigold.service.impl;

import com.josalukkas.digigold.dto.response.ApiResponse;
import com.josalukkas.digigold.entity.GoldPrice;
import com.josalukkas.digigold.entity.User;
import com.josalukkas.digigold.entity.Wallet;
import com.josalukkas.digigold.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.josalukkas.digigold.repository.UserRepository;
import com.josalukkas.digigold.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final GoldPriceService goldPriceService;

    @Transactional(readOnly = true)
    public ApiResponse.WalletResponse getWallet(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        // Best-effort: get live sell price to compute portfolio value
        BigDecimal currentValueInr = BigDecimal.ZERO;
        try {
            GoldPrice price = goldPriceService.getActivePriceEntity();
            currentValueInr = wallet.getGoldBalanceGrams()
                    .multiply(price.getSellPricePerGram())
                    .setScale(2, RoundingMode.HALF_UP);
        } catch (Exception ignored) {
            // If no active price, value = 0
        }

        return ApiResponse.WalletResponse.builder()
                .id(wallet.getId())
                .goldBalanceGrams(wallet.getGoldBalanceGrams())
                .inrBalance(wallet.getInrBalance())
                .totalGoldBought(wallet.getTotalGoldBought())
                .totalGoldSold(wallet.getTotalGoldSold())
                .updatedAt(wallet.getUpdatedAt())
                .currentValueInr(currentValueInr)
                .build();
    }

    // Admin: view any user's wallet
    @Transactional(readOnly = true)
    public ApiResponse.WalletResponse getWalletByUserId(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + userId));
        return ApiResponse.WalletResponse.builder()
                .id(wallet.getId())
                .goldBalanceGrams(wallet.getGoldBalanceGrams())
                .inrBalance(wallet.getInrBalance())
                .totalGoldBought(wallet.getTotalGoldBought())
                .totalGoldSold(wallet.getTotalGoldSold())
                .updatedAt(wallet.getUpdatedAt())
                .currentValueInr(BigDecimal.ZERO)
                .build();
    }
}
