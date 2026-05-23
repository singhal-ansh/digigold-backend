package com.josalukkas.digigold.controller;

import com.josalukkas.digigold.dto.request.AuthRequest;
import com.josalukkas.digigold.dto.response.ApiResponse;
import com.josalukkas.digigold.entity.User;
import com.josalukkas.digigold.repository.UserRepository;
import com.josalukkas.digigold.service.impl.AuthService;
import com.josalukkas.digigold.util.CurrentUserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, token refresh and logout")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final CurrentUserUtil currentUserUtil;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<ApiResponse.AuthToken> register(
            @Valid @RequestBody AuthRequest.Register req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<ApiResponse.AuthToken> login(
            @Valid @RequestBody AuthRequest.Login req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token")
    public ResponseEntity<ApiResponse.AuthToken> refresh(
            @Valid @RequestBody AuthRequest.RefreshTokenRequest req) {
        return ResponseEntity.ok(authService.refreshToken(req.getRefreshToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and invalidate refresh token")
    public ResponseEntity<ApiResponse.MessageResponse> logout(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = currentUserUtil.getCurrentUserId();
        authService.logout(userId);
        return ResponseEntity.ok(ApiResponse.MessageResponse.builder()
                .success(true)
                .message("Logged out successfully")
                .build());
    }

    @PutMapping("/change-password")
    @Operation(summary = "Change authenticated user's password")
    public ResponseEntity<ApiResponse.MessageResponse> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AuthRequest.ChangePassword req) {
        authService.changePassword(currentUserUtil.getCurrentUserId(), req);
        return ResponseEntity.ok(ApiResponse.MessageResponse.builder()
                .success(true).message("Password changed successfully").build());
    }
}
