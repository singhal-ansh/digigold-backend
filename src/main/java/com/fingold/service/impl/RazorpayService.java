package com.fingold.service.impl;

import com.fingold.config.RazorpayConfig;
import com.fingold.exception.GlobalExceptionHandler.PaymentVerificationException;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;

/**
 * Razorpay Integration Service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RazorpayService {

    private final RazorpayConfig razorpayConfig;

    private final RazorpayClient razorpayClient;

    /**
     * Create Razorpay order using actual Razorpay API.
     *
     * @param amountInRupees total INR amount
     * @param orderReference internal order reference
     * @return order details required by frontend
     */
    public Map<String, Object> createOrder(
            BigDecimal amountInRupees,
            String orderReference) {

        try {

            long amountInPaise = amountInRupees
                    .multiply(new BigDecimal("100"))
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValueExact();

            JSONObject options = new JSONObject();

            options.put("amount", amountInPaise);

            options.put("currency", razorpayConfig.getCurrency());

            options.put("receipt", orderReference);

            Order order = razorpayClient.orders.create(options);

            log.info(
                    "Razorpay order created: {} for ₹{} (receipt={})",
                    order.get("id"),
                    amountInRupees,
                    orderReference
            );

            return Map.of(
                    "id", order.get("id"),
                    "amount", order.get("amount"),
                    "currency", order.get("currency"),
                    "receipt", order.get("receipt"),
                    "status", order.get("status"),
                    "key_id", razorpayConfig.getKeyId()
            );

        } catch (Exception e) {

            log.error("Failed to create Razorpay order", e);

            throw new RuntimeException(
                    "Unable to create Razorpay order: " + e.getMessage()
            );
        }
    }

    /**
     * Verify Razorpay payment signature.
     */
    public void verifyPaymentSignature(
            String razorpayOrderId,
            String razorpayPaymentId,
            String razorpaySignature) {

        try {

            String payload =
                    razorpayOrderId + "|" + razorpayPaymentId;

            String expectedSignature =
                    computeHmacSha256(
                            payload,
                            razorpayConfig.getKeySecret()
                    );

            if (!expectedSignature.equalsIgnoreCase(razorpaySignature)) {

                throw new PaymentVerificationException(
                        "Payment signature verification failed."
                );
            }

            log.info(
                    "Payment signature verified successfully for order={}",
                    razorpayOrderId
            );

        } catch (PaymentVerificationException e) {

            throw e;

        } catch (Exception e) {

            log.error("Signature verification error", e);

            throw new PaymentVerificationException(
                    "Failed to verify payment: " + e.getMessage()
            );
        }
    }

    // ─────────────────────────────────────────────

    private String computeHmacSha256(
            String data,
            String secret) throws Exception {

        Mac mac = Mac.getInstance("HmacSHA256");

        SecretKeySpec keySpec = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );

        mac.init(keySpec);

        byte[] hmac = mac.doFinal(
                data.getBytes(StandardCharsets.UTF_8)
        );

        return HexFormat.of().formatHex(hmac);
    }
}