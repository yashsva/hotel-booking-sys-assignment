package com.rupeek.hotelbooking.infrastructure.config;

import com.rupeek.hotelbooking.domain.cancellation.CancellationPolicy;
import com.rupeek.hotelbooking.domain.pricing.PricingStrategy;
import com.rupeek.hotelbooking.infrastructure.policy.BaseNightlyPricing;
import com.rupeek.hotelbooking.infrastructure.policy.TieredCancellationPolicy;
import com.rupeek.hotelbooking.infrastructure.policy.WeekendSurchargePricing;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.LocalTime;

/** Chooses which policy/strategy implementations back the domain interfaces. */
@Configuration
@EnableConfigurationProperties(HotelProperties.class)
public class DomainConfig {

    @Bean
    Clock clock(HotelProperties properties) {
        return Clock.system(properties.zoneId());
    }

    @Bean
    CancellationPolicy cancellationPolicy(HotelProperties properties) {
        HotelProperties.Cancellation c = properties.cancellation();
        return new TieredCancellationPolicy(properties.zoneId(), LocalTime.of(properties.checkInHour(), 0),
                c.fullRefundHours(), c.partialRefundHours(), c.partialRefundPercent());
    }

    @Bean
    @ConditionalOnProperty(name = "hotel.pricing.strategy", havingValue = "weekend-surcharge")
    PricingStrategy weekendSurchargePricing(HotelProperties properties) {
        return new WeekendSurchargePricing(properties.pricing().weekendSurchargePercent());
    }

    @Bean
    @ConditionalOnProperty(name = "hotel.pricing.strategy", havingValue = "base", matchIfMissing = true)
    PricingStrategy basePricing() {
        return new BaseNightlyPricing();
    }
}
