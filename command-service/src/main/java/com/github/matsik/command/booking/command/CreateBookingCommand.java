package com.github.matsik.command.booking.command;

import com.github.matsik.kafka.task.CreateBookingCommandValue;
import com.github.matsik.cassandra.model.BookingPartitionKey;

import java.util.UUID;

public record CreateBookingCommand(
        BookingPartitionKey bookingPartitionKey,
        UUID userId,
        int start,
        int end
) {

    public static class Factory {
        public static CreateBookingCommand create(BookingPartitionKey identifier, CreateBookingCommandValue value) {
            return new CreateBookingCommand(identifier, value.userId(), value.start(), value.end());
        }
    }
}
