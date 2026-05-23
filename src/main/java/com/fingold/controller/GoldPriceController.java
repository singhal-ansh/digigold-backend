package com.fingold.controller;

import com.fingold.dto.request.GoldPriceRequest;
import com.fingold.dto.response.ApiResponse;
import com.fingold.service.impl.GoldPriceService;
import com.fingold.util.CurrentUserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/gold-prices")
@RequiredArgsConstructor
@Tag(name = "Gold Prices", description = "Live gold price and admin price management")
public class GoldPriceController {

    private final GoldPriceService goldPriceService;
    private final CurrentUserUtil currentUserUtil;

    /** Public endpoint — no auth required */
    @GetMapping("/live")
    @Operation(summary = "Get current live gold buy/sell price")
    public ResponseEntity<ApiResponse.GoldPriceResponse> getLivePrice() {
        return ResponseEntity.ok(goldPriceService.getLivePrice());
    }

    /** Admin: manually set a new price */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: set new gold price", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse.GoldPriceResponse> setPrice(
            @Valid @RequestBody GoldPriceRequest req) {
        return ResponseEntity.ok(
                goldPriceService.setPrice(req, currentUserUtil.getCurrentUserEmail()));
    }

    /** Admin: price history */
    @GetMapping("/history")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: gold price history", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Page<ApiResponse.GoldPriceResponse>> getPriceHistory(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(goldPriceService.getPriceHistory(pageable));
    }
}
