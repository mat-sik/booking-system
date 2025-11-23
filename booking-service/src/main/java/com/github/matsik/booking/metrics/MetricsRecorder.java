package com.github.matsik.booking.metrics;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;

import java.util.function.Supplier;

public class MetricsRecorder {

    public static void recordMetrics(LongCounter counter, DoubleHistogram histogram, Runnable operation, String operationName) {
        recordMetrics(counter, histogram, () -> {
            operation.run();
            return null;
        }, operationName);
    }

    public static <T> T recordMetrics(LongCounter counter, DoubleHistogram histogram, Supplier<T> operation, String operationName) {
        long startTime = System.nanoTime();
        T result = operation.get();
        long duration = System.nanoTime() - startTime;

        Attributes attrs = Attributes.of(
                AttributeKey.stringKey("operation"), operationName
        );

        counter.add(1L, attrs);
        histogram.record(duration / 1_000_000.0, attrs);

        return result;
    }

}
