package com.github.matsik.booking.client.command;

import com.github.matsik.booking.client.command.exception.BookingCommandDeliveryException;
import com.github.matsik.kafka.task.CommandValue;
import com.github.matsik.kafka.task.CreateBookingCommandValue;
import com.github.matsik.kafka.task.DeleteBookingCommandValue;
import com.github.matsik.cassandra.model.BookingPartitionKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
@Log
public class CommandClient {
    private static final String TOPIC_NAME = "bookings";
    private static final int TIMEOUT = 10;
    private static final TimeUnit TIMEOUT_TIME_UNIT = TimeUnit.SECONDS;

    private final KafkaTemplate<BookingPartitionKey, CommandValue> template;

    public void sendCreateBookingCommand(BookingPartitionKey key, CreateBookingCommandValue value) {
        send(key, value);
    }

    public void sendDeleteBookingCommand(BookingPartitionKey key, DeleteBookingCommandValue value) {
        send(key, value);
    }

    private void send(BookingPartitionKey key, CommandValue value) {
        try {
            template.send(TOPIC_NAME, key, value).get(TIMEOUT, TIMEOUT_TIME_UNIT);
            // TODO: Improve this logging when adding open telemetry
        } catch (ExecutionException ex) {
            logAndThrow("Failed to deliver the booking command to Kafka", ex);
        } catch (TimeoutException ex) {
            logAndThrow("Timeout occurred while waiting for booking command delivery", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            logAndThrow("Calling thread was interrupted", ex);
        } catch (CancellationException ex) {
            logAndThrow("Future was canceled", ex);
        }
    }

    private void logAndThrow(String message, Exception ex) {
        log.severe(message + ", with cause: " + ex.getMessage());
        throw new BookingCommandDeliveryException(ex);
    }

}
