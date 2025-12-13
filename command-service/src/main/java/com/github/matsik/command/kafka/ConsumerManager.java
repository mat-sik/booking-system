package com.github.matsik.command.kafka;

import com.github.matsik.command.adapter.in.kafka.BookingCommandListenerAdapter;
import com.github.matsik.command.adapter.out.cassandra.BookingCache;
import com.github.matsik.command.adapter.out.cassandra.BookingPersistenceCachingAdapter;
import com.github.matsik.command.adapter.out.cassandra.BookingPersistenceService;
import com.github.matsik.command.application.domain.CreateBookingService;
import com.github.matsik.command.application.domain.DeleteBookingService;
import com.github.matsik.command.config.kafka.KafkaProperties;
import com.github.matsik.dto.BookingPartitionKey;
import com.github.matsik.kafka.task.CommandValue;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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

    private final int consumerCount;
    private final CountDownLatch shutdownLatch;
    private final List<Future<?>> runningConsumers;

    private final Properties kafkaConsumerProperties;

    private final String bookingTopicName;
    private final TopicCreator topicCreator;

    private final long pollTimeoutMs;

    private final LongCounter batchCounter;
    private final DoubleHistogram batchHistogram;

    private final LongCounter recordCounter;
    private final DoubleHistogram recordHistogram;

    BookingPersistenceService bookingPersistenceService;

    public ConsumerManager(
            Properties kafkaConsumerProperties,
            KafkaProperties kafkaProperties,
            TopicCreator topicCreator,
            LongCounter batchCounter,
            DoubleHistogram batchHistogram,
            LongCounter recordCounter,
            DoubleHistogram recordHistogram,
            BookingPersistenceService bookingPersistenceService
    ) {
        this.running = new AtomicBoolean();
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();

        this.consumerCount = kafkaProperties.consumer().concurrentConsumerCount();
        this.shutdownLatch = new CountDownLatch(consumerCount);
        this.runningConsumers = new ArrayList<>();

        this.kafkaConsumerProperties = kafkaConsumerProperties;

        this.bookingTopicName = kafkaProperties.topics().bookingTopicName();
        this.topicCreator = topicCreator;

        this.pollTimeoutMs = kafkaProperties.consumer().pollTimeoutMs();

        this.batchCounter = batchCounter;
        this.batchHistogram = batchHistogram;

        this.recordCounter = recordCounter;
        this.recordHistogram = recordHistogram;

        this.bookingPersistenceService = bookingPersistenceService;
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

            BookingCache bookingCache = new BookingCache(bookingPersistenceService, new HashMap<>());

            ConsumerRebalanceListener bookingConsumerRebalanceListener = new BookingConsumerRebalanceListener(bookingCache);

            consumer.subscribe(Collections.singletonList(bookingTopicName), bookingConsumerRebalanceListener);

            ConsumerRunner consumerRunner = consumerRunner(consumer, bookingCache);

            Future<?> runningConsumer = executorService.submit(consumerRunner);
            runningConsumers.add(runningConsumer);
        }
        log.info("Consumer manager is started");
    }

    private ConsumerRunner consumerRunner(Consumer<BookingPartitionKey, CommandValue> consumer, BookingCache bookingCache) {
        BookingPersistenceCachingAdapter bookingPersistenceCachingAdapter = new BookingPersistenceCachingAdapter(bookingPersistenceService, bookingCache);

        CreateBookingService createBookingService = new CreateBookingService(bookingPersistenceCachingAdapter, recordCounter, recordHistogram);
        DeleteBookingService deleteBookingService = new DeleteBookingService(bookingPersistenceCachingAdapter, recordCounter, recordHistogram);

        RecordsHandler recordsHandler = new BookingCommandListenerAdapter(createBookingService, deleteBookingService, batchCounter, batchHistogram);

        return new ConsumerRunner(consumer, recordsHandler, pollTimeoutMs, shutdownLatch);
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
