package com.fingold.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class GoldPriceRequest {

    @NotNull
    @DecimalMin(value = "1000.00", message = "Buy price per gram must be at least ₹1000")
    private BigDecimal buyPricePerGram;

    @NotNull
    @DecimalMin(value = "1000.00", message = "Sell price per gram must be at least ₹1000")
    private BigDecimal sellPricePerGram;

    @NotNull
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "30.0")
    private BigDecimal gstPercentage;

    @Min(60)
    @Max(3600)
    private Integer validitySeconds = 300;

    @Size(max = 50)
    private String source = "MANUAL";
}
