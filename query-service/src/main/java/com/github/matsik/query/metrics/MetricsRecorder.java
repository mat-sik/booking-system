package com.github.matsik.query.metrics;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;

public class MetricsRecorder {

    public static void recordMetrics(LongCounter counter, DoubleHistogram histogram, Runnable operation, String operationName) {
        long startTime = System.nanoTime();
        operation.run();
        long duration = System.nanoTime() - startTime;

        Attributes attrs = Attributes.of(
                AttributeKey.stringKey("operation"), operationName
        );

        counter.add(1L, attrs);
        histogram.record(duration / 1_000_000.0, attrs);
    }

}
