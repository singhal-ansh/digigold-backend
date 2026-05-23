package com.fingold.service.impl;

import com.fingold.dto.response.ApiResponse;
import com.fingold.entity.User;
import com.fingold.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.fingold.repository.TransactionRepository;
import com.fingold.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public ApiResponse.UserProfile getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return AuthService.toProfile(user);
    }

    @Transactional
    public ApiResponse.UserProfile updateProfile(String email, String fullName, String phone) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (fullName != null && !fullName.isBlank()) user.setFullName(fullName);
        if (phone != null && !phone.isBlank()) user.setPhone(phone);

        return AuthService.toProfile(userRepository.save(user));
    }

    // ── Admin ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ApiResponse.UserProfile> getAllUsers(String search, Pageable pageable) {
        Page<User> page = (search != null && !search.isBlank())
                ? userRepository.searchUsers(search, pageable)
                : userRepository.findAll(pageable);
        return page.map(AuthService::toProfile);
    }

    @Transactional(readOnly = true)
    public ApiResponse.UserProfile getUserById(Long id) {
        return AuthService.toProfile(
                userRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id)));
    }

    @Transactional
    public ApiResponse.UserProfile toggleUserEnabled(Long id, boolean enabled) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        user.setEnabled(enabled);
        return AuthService.toProfile(userRepository.save(user));
    }

    @Transactional
    public ApiResponse.UserProfile promoteToAdmin(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        user.setRole(User.Role.ADMIN);
        return AuthService.toProfile(userRepository.save(user));
    }

    @Transactional
    public ApiResponse.UserProfile updateKycStatus(Long id, String kycStatus) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        user.setKycStatus(kycStatus);
        return AuthService.toProfile(userRepository.save(user));
    }


    @Transactional
    public ApiResponse.UserProfile submitKyc(String email, String panNumber) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (panNumber == null || panNumber.isBlank()) {
            throw new IllegalArgumentException("PAN number is required");
        }
        user.setPanNumber(panNumber.toUpperCase().trim());
        // Only move to UNDER_REVIEW if currently PENDING or REJECTED
        if (!"VERIFIED".equals(user.getKycStatus())) {
            user.setKycStatus("UNDER_REVIEW");
        }
        return AuthService.toProfile(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public ApiResponse.AdminStats getDashboardStats(GoldPriceService goldPriceService) {
        long totalUsers = userRepository.countByRole(User.Role.USER);
        long totalTx = transactionRepository.countCompleted();
        var totalRevenue = transactionRepository.sumCompletedBuyAmount();
        var goldSold = transactionRepository.sumGoldBoughtGrams();

        ApiResponse.GoldPriceResponse activePrices = null;
        try {
            activePrices = goldPriceService.getLivePrice();
        } catch (Exception ignored) {}

        return ApiResponse.AdminStats.builder()
                .totalUsers(totalUsers)
                .totalTransactions(totalTx)
                .totalGoldSoldGrams(goldSold)
                .totalRevenueInr(totalRevenue)
                .activePrices(activePrices)
                .build();
    }
}
