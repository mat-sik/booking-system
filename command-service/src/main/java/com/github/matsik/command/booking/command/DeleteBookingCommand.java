package com.github.matsik.command.booking.command;

import com.github.matsik.kafka.task.DeleteBookingCommandValue;
import com.github.matsik.cassandra.model.BookingPartitionKey;

import java.util.UUID;

public record DeleteBookingCommand(BookingPartitionKey bookingPartitionKey, UUID bookingId) {
    public static class Factory {
        public static DeleteBookingCommand create(BookingPartitionKey identifier, DeleteBookingCommandValue value) {
            return new DeleteBookingCommand(identifier, value.bookingId());
        }
    }
}
