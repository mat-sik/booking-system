package com.github.matsik.booking.command;

import com.github.matsik.kafka.task.CommandValue;
import com.github.matsik.kafka.task.CreateBookingCommandValue;
import com.github.matsik.kafka.task.DeleteBookingCommandValue;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class CommandClient {
    private static final String TOPIC_NAME = "bookings";

    private final KafkaTemplate<LocalDate, CommandValue> template;

    public void sendCreateBookingCommand(LocalDate key, CreateBookingCommandValue value) {
        if (value.start() >= value.end()) {
            throw new IllegalArgumentException("Booking start value should be lower than the end value.");
        }
        template.send(TOPIC_NAME, key, value);
    }

    public void sendDeleteBookingCommand(LocalDate key, DeleteBookingCommandValue value) {
        template.send(TOPIC_NAME, key, value);
    }

}
