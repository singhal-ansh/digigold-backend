package com.josalukkas.digigold.controller;

import com.josalukkas.digigold.dto.response.ApiResponse;
import com.josalukkas.digigold.service.impl.WalletService;
import com.josalukkas.digigold.util.CurrentUserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
@Tag(name = "Wallet", description = "Gold wallet balance and portfolio value")
@SecurityRequirement(name = "bearerAuth")
public class WalletController {

    private final WalletService walletService;
    private final CurrentUserUtil currentUserUtil;

    @GetMapping
    @Operation(summary = "Get authenticated user's gold wallet")
    public ResponseEntity<ApiResponse.WalletResponse> getMyWallet() {
        return ResponseEntity.ok(
                walletService.getWallet(currentUserUtil.getCurrentUserEmail()));
    }
}
