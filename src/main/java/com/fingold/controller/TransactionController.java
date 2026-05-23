package com.fingold.controller;

import com.fingold.dto.request.TransactionRequest;
import com.fingold.dto.response.ApiResponse;
import com.fingold.service.impl.TransactionService;
import com.fingold.util.CurrentUserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Buy gold, sell gold, payment verification and order history")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionService transactionService;
    private final CurrentUserUtil currentUserUtil;

    // ── Buy ─────────────────────────────────────────────────────

    @PostMapping("/buy")
    @Operation(summary = "Initiate a gold purchase — returns Razorpay order details")
    public ResponseEntity<ApiResponse.BuyOrderCreated> initiateBuy(
            @Valid @RequestBody TransactionRequest.BuyRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.initiateBuy(req, currentUserUtil.getCurrentUserEmail()));
    }

    @PostMapping("/buy/verify-payment")
    @Operation(summary = "Verify Razorpay payment and credit gold to wallet")
    public ResponseEntity<ApiResponse.TransactionResponse> verifyPayment(
            @Valid @RequestBody TransactionRequest.PaymentVerification req) {
        return ResponseEntity.ok(
                transactionService.verifyPayment(req, currentUserUtil.getCurrentUserEmail()));
    }

    // ── Sell ─────────────────────────────────────────────────────

    @PostMapping("/sell")
    @Operation(summary = "Sell gold from wallet — instant settlement")
    public ResponseEntity<ApiResponse.SellOrderCreated> sell(
            @Valid @RequestBody TransactionRequest.SellRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.initiateSell(req, currentUserUtil.getCurrentUserEmail()));
    }

    // ── Order History ─────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "Get current user's transaction history")
    public ResponseEntity<Page<ApiResponse.TransactionResponse>> getMyTransactions(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(
                transactionService.getUserTransactions(
                        currentUserUtil.getCurrentUserEmail(), pageable));
    }

    @GetMapping("/{orderRef}")
    @Operation(summary = "Get single transaction detail by order reference")
    public ResponseEntity<ApiResponse.TransactionResponse> getTransaction(
            @PathVariable String orderRef) {
        return ResponseEntity.ok(
                transactionService.getTransactionDetail(
                        orderRef, currentUserUtil.getCurrentUserEmail()));
    }
}
