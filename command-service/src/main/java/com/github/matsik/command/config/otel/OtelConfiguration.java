package com.github.matsik.command.config.otel;

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
        return openTelemetry.getMeter("command-service");
    }

    @Bean
    public LongCounter recordCounter(Meter meter) {
        return meter.counterBuilder("command.service.records")
                .setDescription("Total Command Service records")
                .setUnit("requests")
                .build();
    }

    @Bean
    public DoubleHistogram recordHistogram(Meter meter) {
        return meter.histogramBuilder("command.service.record.processing.duration")
                .setDescription("Duration of handling a Command Service record")
                .setUnit("ms")
                .build();
    }

    @Bean
    public LongCounter batchCounter(Meter meter) {
        return meter.counterBuilder("command.service.batches")
                .setDescription("Total Command Service batches")
                .setUnit("batches")
                .build();
    }

    @Bean
    public DoubleHistogram batchHistogram(Meter meter) {
        return meter.histogramBuilder("command.service.batch.processing.duration")
                .setDescription("Duration of handling a Command Service batch")
                .setUnit("ms")
                .build();
    }

}
