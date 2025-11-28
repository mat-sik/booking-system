package com.github.matsik.booking.config.otel;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OtelConfiguration {

    @Bean
    public OpenTelemetry openTelemetry() {
        return GlobalOpenTelemetry.get();
    }

    @Bean
    public Meter meter(OpenTelemetry openTelemetry) {
        return openTelemetry.getMeter("booking-service");
    }

    @Bean
    public LongCounter requestCounter(Meter meter) {
        return meter.counterBuilder("booking.service.requests")
                .setDescription("Total Booking Service %s requests")
                .setUnit("requests")
                .build();
    }

    @Bean
    public DoubleHistogram requestHistogram(Meter meter) {
        return meter.histogramBuilder("booking.service.request.duration")
                .setDescription("Duration of handling a Booking Service %s request")
                .setUnit("ms")
                .build();
    }

}
