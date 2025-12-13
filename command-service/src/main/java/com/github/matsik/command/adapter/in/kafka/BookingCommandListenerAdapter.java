package com.github.matsik.command.adapter.in.kafka;

import com.github.matsik.command.application.port.in.CreateBookingCommand;
import com.github.matsik.command.application.port.in.CreateBookingUseCase;
import com.github.matsik.command.application.port.in.DeleteBookingCommand;
import com.github.matsik.command.application.port.in.DeleteBookingUseCase;
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

import static com.github.matsik.command.metrics.MetricsRecorder.recordMetrics;

@RequiredArgsConstructor
public class BookingCommandListenerAdapter implements RecordsHandler {

    private final CreateBookingUseCase createBookingUseCase;
    private final DeleteBookingUseCase deleteBookingUseCase;

    private final LongCounter recordCounter;
    private final DoubleHistogram recordHistogram;

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
                createBooking(command);
            }
            case DeleteBookingCommandValue delete -> {
                DeleteBookingCommand command = DeleteBookingCommand.of(key, delete);
                deleteBooking(command);
            }
        }
    }

    private void createBooking(CreateBookingCommand command) {
        recordMetrics(recordCounter, recordHistogram, () -> createBookingUseCase.createBooking(command), "create_booking");
    }

    public void deleteBooking(DeleteBookingCommand command) {
        recordMetrics(recordCounter, recordHistogram, () -> deleteBookingUseCase.deleteBooking(command), "delete_booking");
    }
}
