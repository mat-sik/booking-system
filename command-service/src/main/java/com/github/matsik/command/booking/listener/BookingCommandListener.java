package com.github.matsik.command.booking.listener;

import com.github.matsik.command.booking.command.CreateBookingCommand;
import com.github.matsik.command.booking.command.DeleteBookingCommand;
import com.github.matsik.command.booking.service.BookingService;
import com.github.matsik.command.kafka.RecordsHandler;
import com.github.matsik.dto.BookingPartitionKey;
import com.github.matsik.kafka.task.CommandValue;
import com.github.matsik.kafka.task.CreateBookingCommandValue;
import com.github.matsik.kafka.task.DeleteBookingCommandValue;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.stereotype.Component;

import static com.github.matsik.command.metrics.MetricsRecorder.recordMetrics;

@Component
@RequiredArgsConstructor
public class BookingCommandListener implements RecordsHandler {

    private final BookingService service;

    private final LongCounter batchCounter;
    private final DoubleHistogram batchHistogram;

    @Override
    public void onRecords(ConsumerRecords<BookingPartitionKey, CommandValue> records) {
        recordMetrics(batchCounter, batchHistogram,
                () -> records.forEach(this::processRecord),
                "process_batch"
        );
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

}
