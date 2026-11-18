package com.rupeek.hotelbooking.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/**
 * {@code method} is CARD, UPI or WALLET. {@code details} depend on the method:
 * CARD -> cardToken, UPI -> vpa, WALLET -> walletId.
 */
public record PayRequest(@NotBlank String method, Map<String, String> details) {
}
