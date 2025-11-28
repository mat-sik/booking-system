package com.github.matsik.query.config.otel;

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
        return openTelemetry.getMeter("query-service");
    }

    @Bean
    public LongCounter requestCounter(Meter meter) {
        return meter.counterBuilder("query.service.requests")
                .setDescription("Total Query Service requests")
                .setUnit("requests")
                .build();
    }

    @Bean
    public DoubleHistogram requestHistogram(Meter meter) {
        return meter.histogramBuilder("query.service.request.duration")
                .setDescription("Duration of handling a Query Service request")
                .setUnit("ms")
                .build();
    }

}
