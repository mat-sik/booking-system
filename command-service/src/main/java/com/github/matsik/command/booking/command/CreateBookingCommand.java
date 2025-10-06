package com.github.matsik.command.booking.command;

import com.github.matsik.dto.BookingPartitionKey;
import com.github.matsik.kafka.task.CreateBookingCommandValue;
import lombok.Builder;

import java.util.UUID;

@Builder
public record CreateBookingCommand(
        BookingPartitionKey bookingPartitionKey,
        UUID userId,
        int start,
        int end
) {

    public static class Factory {
        public static CreateBookingCommand create(BookingPartitionKey key, CreateBookingCommandValue value) {
            return CreateBookingCommand.builder()
                    .bookingPartitionKey(key)
                    .userId(value.userId())
                    .start(value.start())
                    .end(value.end())
                    .build();
        }
    }
}
