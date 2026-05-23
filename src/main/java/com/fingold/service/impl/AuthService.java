package com.fingold.service.impl;

import com.fingold.dto.request.AuthRequest;
import com.fingold.dto.response.ApiResponse;
import com.fingold.entity.RefreshToken;
import com.fingold.entity.User;
import com.fingold.entity.Wallet;
import com.fingold.exception.GlobalExceptionHandler.*;
import com.fingold.repository.RefreshTokenRepository;
import com.fingold.repository.UserRepository;
import com.fingold.repository.WalletRepository;
import com.fingold.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    // ── Register ─────────────────────────────────────────────────

    @Transactional
    public ApiResponse.AuthToken register(AuthRequest.Register req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + req.getEmail());
        }
        if (req.getPhone() != null && userRepository.existsByPhone(req.getPhone())) {
            throw new DuplicateResourceException("Phone already registered: " + req.getPhone());
        }

        User user = User.builder()
                .fullName(req.getFullName())
                .email(req.getEmail().toLowerCase())
                .phone(req.getPhone())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(User.Role.USER)
                .enabled(true)
                .emailVerified(false)
                .kycStatus("PENDING")
                .build();

        user = userRepository.save(user);

        // Create a wallet for the new user
        Wallet wallet = Wallet.builder().user(user).build();
        walletRepository.save(wallet);

        log.info("New user registered: {} (id={})", user.getEmail(), user.getId());

        return buildAuthToken(user);
    }

    // ── Login ─────────────────────────────────────────────────────

    @Transactional
    public ApiResponse.AuthToken login(AuthRequest.Login req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));

        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        log.info("User logged in: {}", user.getEmail());
        return buildAuthToken(user);
    }

    // ── Refresh token ─────────────────────────────────────────────

    @Transactional
    public ApiResponse.AuthToken refreshToken(String refreshTokenValue) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));

        if (storedToken.isExpired()) {
            refreshTokenRepository.delete(storedToken);
            throw new InvalidRefreshTokenException("Refresh token expired — please log in again");
        }

        User user = storedToken.getUser();
        // Rotate the refresh token (issue a new one)
        refreshTokenRepository.delete(storedToken);

        return buildAuthToken(user);
    }

    // ── Logout ───────────────────────────────────────────────────

    @Transactional
    public void logout(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    // ── Change password ──────────────────────────────────────────

    @Transactional
    public void changePassword(Long userId, AuthRequest.ChangePassword req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword())) {
            throw new InvalidRefreshTokenException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
        // Invalidate all sessions
        refreshTokenRepository.deleteByUserId(userId);
        log.info("Password changed for user id={}", userId);
    }

    // ── Helpers ──────────────────────────────────────────────────

    private ApiResponse.AuthToken buildAuthToken(User user) {
        UserDetails ud = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtUtils.generateToken(ud);

        // Upsert refresh token
        RefreshToken rt = refreshTokenRepository.findByUserId(user.getId())
                .orElse(RefreshToken.builder().user(user).build());
        rt.setToken(UUID.randomUUID().toString());
        rt.setExpiryDate(Instant.now().plusMillis(refreshExpirationMs));
        refreshTokenRepository.save(rt);

        return ApiResponse.AuthToken.builder()
                .accessToken(accessToken)
                .refreshToken(rt.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtExpirationMs / 1000)
                .user(toProfile(user))
                .build();
    }

    public static ApiResponse.UserProfile toProfile(User u) {
        return ApiResponse.UserProfile.builder()
                .id(u.getId())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .phone(u.getPhone())
                .role(u.getRole())
                .emailVerified(u.isEmailVerified())
                .kycStatus(u.getKycStatus())
                .createdAt(u.getCreatedAt())
                .build();
    }
}
