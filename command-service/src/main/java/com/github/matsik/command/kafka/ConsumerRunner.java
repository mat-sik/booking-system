package com.github.matsik.command.kafka;

import com.github.matsik.dto.BookingPartitionKey;
import com.github.matsik.kafka.task.CommandValue;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class ConsumerRunner implements Runnable {

    private final Consumer<BookingPartitionKey, CommandValue> consumer;
    private final RecordsHandler recordsHandler;

    private final Duration pollTimeout;

    private final CountDownLatch shutdownLatch;

    public ConsumerRunner(
            Consumer<BookingPartitionKey, CommandValue> consumer,
            RecordsHandler recordsHandler,
            long pollTimeoutMs,
            CountDownLatch shutdownLatch
    ) {
        this.consumer = consumer;
        this.recordsHandler = recordsHandler;
        this.pollTimeout = Duration.ofMillis(pollTimeoutMs);
        this.shutdownLatch = shutdownLatch;
    }

    @Override
    public void run() {
        log.info("Consumer thread started");
        try {
            while (!Thread.currentThread().isInterrupted()) {
                ConsumerRecords<BookingPartitionKey, CommandValue> records = consumer.poll(pollTimeout);
                recordsHandler.onRecords(records);
                consumer.commitSync();
            }
        } finally {
            consumer.close();
            shutdownLatch.countDown();
            log.info("Shutting down consumer thread");
        }
    }

}
