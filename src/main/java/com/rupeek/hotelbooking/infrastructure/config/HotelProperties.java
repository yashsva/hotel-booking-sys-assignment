package com.rupeek.hotelbooking.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.ZoneId;

/** Business settings bound from {@code hotel.*} in application.properties. */
@ConfigurationProperties(prefix = "hotel")
public record HotelProperties(ZoneId zoneId, int checkInHour, Cancellation cancellation, Pricing pricing) {

    public record Cancellation(long fullRefundHours, long partialRefundHours, int partialRefundPercent) {
    }

    /** {@code strategy} is {@code base} or {@code weekend-surcharge}. */
    public record Pricing(String strategy, int weekendSurchargePercent) {
    }
}
