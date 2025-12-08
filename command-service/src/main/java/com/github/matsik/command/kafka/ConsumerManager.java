package com.github.matsik.command.kafka;

import com.github.matsik.command.config.kafka.KafkaProperties;
import com.github.matsik.dto.BookingPartitionKey;
import com.github.matsik.kafka.task.CommandValue;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Slf4j
public class ConsumerManager implements SmartLifecycle {

    private final AtomicBoolean running;
    private final ExecutorService executorService;

    private final Properties kafkaConsumerProperties;

    private final RecordsHandler recordsHandler;

    private final TopicCreator topicCreator;

    private final int consumerCount;
    private final CountDownLatch shutdownLatch;
    private final List<Future<?>> runningConsumers;

    private final String bookingTopicName;

    private final long pollTimeoutMs;

    public ConsumerManager(
            Properties kafkaConsumerProperties,
            KafkaProperties kafkaProperties,
            RecordsHandler recordsHandler,
            TopicCreator topicCreator
    ) {
        this.running = new AtomicBoolean();
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();

        this.kafkaConsumerProperties = kafkaConsumerProperties;

        this.recordsHandler = recordsHandler;

        this.topicCreator = topicCreator;

        this.consumerCount = kafkaProperties.consumer().concurrentConsumerCount();
        this.shutdownLatch = new CountDownLatch(consumerCount);
        this.runningConsumers = new ArrayList<>();

        this.bookingTopicName = kafkaProperties.topics().bookingTopicName();

        this.pollTimeoutMs = kafkaProperties.consumer().pollTimeoutMs();
    }

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            log.info("Consumer manager is already started");
            return;
        }
        log.info("Starting consumer manager");

        topicCreator.ensureBookingTopicExists();

        for (int i = 0; i < consumerCount; i++) {
            Consumer<BookingPartitionKey, CommandValue> consumer = new KafkaConsumer<>(kafkaConsumerProperties);
            consumer.subscribe(Collections.singletonList(bookingTopicName));

            ConsumerRunner consumerRunner = new ConsumerRunner(consumer, recordsHandler, pollTimeoutMs, shutdownLatch);

            Future<?> runningConsumer = executorService.submit(consumerRunner);
            runningConsumers.add(runningConsumer);
        }
        log.info("Consumer manager is started");
    }

    @Override
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            log.info("Consumer manager is already stopped");
            return;
        }
        log.info("Stopping consumer manager");

        for (Future<?> runningConsumer : runningConsumers) {
            runningConsumer.cancel(true);
        }
        try {
            log.info("Waiting for consumers to stop");
            boolean latchReachedZero = shutdownLatch.await(5, TimeUnit.SECONDS);
            if (latchReachedZero) {
                log.info("Consumers have been stopped");
            } else {
                log.warn("Consumers have not been stopped gracefully");
            }
        } catch (InterruptedException ex) {
            log.error("Interrupted while waiting for consumers to gracefully stop.", ex);
        } finally {
            log.info("Shutting down consumer manager");
            executorService.shutdown();
            log.info("Shut down consumer manager");
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

}
