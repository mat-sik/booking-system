package com.github.matsik.command.booking.command;

import com.github.matsik.dto.BookingPartitionKey;
import com.github.matsik.dto.TimeRange;
import com.github.matsik.kafka.task.CreateBookingCommandValue;
import lombok.Builder;

import java.util.UUID;

@Builder
public record CreateBookingCommand(
        BookingPartitionKey bookingPartitionKey,
        UUID userId,
        TimeRange timeRange
) {
    public static CreateBookingCommand of(BookingPartitionKey key, CreateBookingCommandValue value) {
        return CreateBookingCommand.builder()
                .bookingPartitionKey(key)
                .userId(value.userId())
                .timeRange(TimeRange.of(value.start(), value.end()))
                .build();
    }
}
