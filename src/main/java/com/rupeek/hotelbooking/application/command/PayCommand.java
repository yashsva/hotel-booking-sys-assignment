package com.rupeek.hotelbooking.application.command;

import java.util.Map;

public record PayCommand(String bookingId, String idempotencyKey, String method, Map<String, String> details) {
}
