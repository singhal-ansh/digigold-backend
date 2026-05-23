package com.fingold.controller;

import com.fingold.dto.response.ApiResponse;
import com.fingold.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.fingold.service.impl.BankAccountService;
import com.fingold.service.impl.UserService;
import com.fingold.util.CurrentUserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "User profile management")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;
    private final BankAccountService bankAccountService;
    private final CurrentUserUtil currentUserUtil;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ApiResponse.UserProfile> getMyProfile() {
        return ResponseEntity.ok(
                userService.getProfile(currentUserUtil.getCurrentUserEmail()));
    }

    @PatchMapping("/me")
    @Operation(summary = "Update name or phone number")
    public ResponseEntity<ApiResponse.UserProfile> updateMyProfile(
            @RequestBody UpdateProfileRequest req) {
        return ResponseEntity.ok(
                userService.updateProfile(
                        currentUserUtil.getCurrentUserEmail(),
                        req.getFullName(),
                        req.getPhone()));
    }

    // ── KYC submission ────────────────────────────────────────────

    @PostMapping("/me/kyc")
    @Operation(summary = "Submit PAN number for KYC verification")
    public ResponseEntity<ApiResponse.UserProfile> submitKyc(
            @RequestBody KycSubmitRequest req) {
        return ResponseEntity.ok(
                userService.submitKyc(currentUserUtil.getCurrentUserEmail(), req.getPanNumber()));
    }

    // ── Bank Account ──────────────────────────────────────────────

    @GetMapping("/me/bank-account")
    @Operation(summary = "Get linked bank account")
    public ResponseEntity<ApiResponse.BankAccountResponse> getMyBankAccount() {
        return ResponseEntity.ok(
                bankAccountService.getByEmail(currentUserUtil.getCurrentUserEmail()));
    }

    @PutMapping("/me/bank-account")
    @Operation(summary = "Add or update linked bank account")
    public ResponseEntity<ApiResponse.BankAccountResponse> saveBankAccount(
            @RequestBody BankAccountRequest req) {
        return ResponseEntity.ok(
                bankAccountService.save(
                        currentUserUtil.getCurrentUserEmail(),
                        req.getAccountNumber(),
                        req.getIfscCode(),
                        req.getAccountHolderName(),
                        req.getBankName()));
    }

    // ── Inner request classes ─────────────────────────────────────

    @Data
    static class UpdateProfileRequest {
        private String fullName;
        private String phone;
    }

    @Data
    static class KycSubmitRequest {
        private String panNumber;
    }

    @Data
    static class BankAccountRequest {
        private String accountNumber;
        private String ifscCode;
        private String accountHolderName;
        private String bankName;
    }
}
