package com.github.matsik.command.booking.command;

import com.github.matsik.dto.BookingPartitionKey;
import com.github.matsik.kafka.task.DeleteBookingCommandValue;
import lombok.Builder;

import java.util.UUID;

@Builder
public record DeleteBookingCommand(BookingPartitionKey bookingPartitionKey, UUID bookingId, UUID userId) {
    public static DeleteBookingCommand of(BookingPartitionKey key, DeleteBookingCommandValue value) {
        return DeleteBookingCommand.builder()
                .bookingPartitionKey(key)
                .bookingId(value.bookingId())
                .userId(value.userId())
                .build();
    }
}
