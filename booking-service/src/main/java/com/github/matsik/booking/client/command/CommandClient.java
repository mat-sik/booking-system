package com.github.matsik.booking.client.command;

import com.github.matsik.booking.client.command.exception.BookingCommandDeliveryException;
import com.github.matsik.dto.BookingPartitionKey;
import com.github.matsik.kafka.task.CommandValue;
import com.github.matsik.kafka.task.CreateBookingCommandValue;
import com.github.matsik.kafka.task.DeleteBookingCommandValue;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.github.matsik.booking.config.kafka.KafkaClientConfiguration.BOOKINGS_TOPIC_NAME;

@Component
@RequiredArgsConstructor
public class CommandClient {
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
            template.send(BOOKINGS_TOPIC_NAME, key, value).get(TIMEOUT, TIMEOUT_TIME_UNIT);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new BookingCommandDeliveryException(e);
        }
    }

}
