package com.github.matsik.command.booking.listener;

import com.github.matsik.command.booking.command.CreateBookingCommand;
import com.github.matsik.command.booking.command.DeleteBookingCommand;
import com.github.matsik.command.booking.service.BookingService;
import com.github.matsik.dto.BookingPartitionKey;
import com.github.matsik.kafka.task.CommandValue;
import com.github.matsik.kafka.task.CreateBookingCommandValue;
import com.github.matsik.kafka.task.DeleteBookingCommandValue;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BookingCommandListener {

    private final BookingService service;
    private final Meter meter;

    @KafkaListener(topics = "bookings", groupId = "${kafka.groupId}", containerFactory = "kafkaListenerContainerFactory")
    public void listen(List<ConsumerRecord<BookingPartitionKey, CommandValue>> records, Acknowledgment ack) {
        recordMetrics(() -> {
            records.forEach(this::processRecord);
            ack.acknowledge();
        });
    }

    private void processRecord(ConsumerRecord<BookingPartitionKey, CommandValue> record) {
        BookingPartitionKey key = record.key();
        CommandValue value = record.value();

        switch (value) {
            case CreateBookingCommandValue create -> {
                CreateBookingCommand command = CreateBookingCommand.of(key, create);
                service.createBooking(command);
            }
            case DeleteBookingCommandValue delete -> {
                DeleteBookingCommand command = DeleteBookingCommand.of(key, delete);
                service.deleteBooking(command);
            }
        }
    }

    private void recordMetrics(Runnable operation) {
        long startTime = System.nanoTime();
        operation.run();
        long duration = System.nanoTime() - startTime;

        recordDurationAndIncrementCounter(meter, duration);
    }

    private void recordDurationAndIncrementCounter(Meter meter, long durationNs) {
        LongCounter counter = meter.counterBuilder("record.batches")
                .setDescription("Total record batches processed")
                .setUnit("batches")
                .build();

        DoubleHistogram histogram = meter.histogramBuilder("record.batches.processing.duration")
                .setDescription("Duration of handling a record batch")
                .setUnit("ms")
                .build();

        counter.add(1L);
        histogram.record(durationNs / 1_000_000.0);
    }

}
