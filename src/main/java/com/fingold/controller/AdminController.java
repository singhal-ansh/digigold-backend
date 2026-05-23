package com.fingold.controller;

import com.fingold.dto.response.ApiResponse;
import com.fingold.service.impl.GoldPriceService;
import com.fingold.service.impl.TransactionService;
import com.fingold.service.impl.UserService;
import com.fingold.service.impl.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin Panel", description = "Admin-only APIs for managing users and transactions")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final UserService userService;
    private final TransactionService transactionService;
    private final WalletService walletService;
    private final GoldPriceService goldPriceService;

    // ── Dashboard ─────────────────────────────────────────────────

    @GetMapping("/dashboard")
    @Operation(summary = "Admin dashboard stats")
    public ResponseEntity<ApiResponse.AdminStats> getDashboard() {
        return ResponseEntity.ok(userService.getDashboardStats(goldPriceService));
    }

    // ── User Management ───────────────────────────────────────────

    @GetMapping("/users")
    @Operation(summary = "List all users (with optional search)")
    public ResponseEntity<Page<ApiResponse.UserProfile>> listUsers(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUsers(search, pageable));
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<ApiResponse.UserProfile> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PatchMapping("/users/{id}/enable")
    @Operation(summary = "Enable a user account")
    public ResponseEntity<ApiResponse.UserProfile> enableUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.toggleUserEnabled(id, true));
    }

    @PatchMapping("/users/{id}/disable")
    @Operation(summary = "Disable (suspend) a user account")
    public ResponseEntity<ApiResponse.UserProfile> disableUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.toggleUserEnabled(id, false));
    }

    @PatchMapping("/users/{id}/promote-admin")
    @Operation(summary = "Promote user to ADMIN role")
    public ResponseEntity<ApiResponse.UserProfile> promoteAdmin(@PathVariable Long id) {
        return ResponseEntity.ok(userService.promoteToAdmin(id));
    }

    @PatchMapping("/users/{id}/kyc")
    @Operation(summary = "Update KYC status (PENDING / VERIFIED / REJECTED)")
    public ResponseEntity<ApiResponse.UserProfile> updateKyc(
            @PathVariable Long id, @RequestBody KycRequest req) {
        return ResponseEntity.ok(userService.updateKycStatus(id, req.getStatus()));
    }

    @GetMapping("/users/{id}/wallet")
    @Operation(summary = "View any user's wallet")
    public ResponseEntity<ApiResponse.WalletResponse> getUserWallet(@PathVariable Long id) {
        return ResponseEntity.ok(walletService.getWalletByUserId(id));
    }

    // ── Transaction Management ────────────────────────────────────

    @GetMapping("/transactions")
    @Operation(summary = "List all transactions across all users")
    public ResponseEntity<Page<ApiResponse.TransactionResponse>> listTransactions(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(transactionService.getAllTransactions(pageable));
    }

    // ── Inner request classes ─────────────────────────────────────

    @Data
    static class KycRequest {
        private String status;  // PENDING, VERIFIED, REJECTED
    }
}
