package com.github.matsik.command.application.port.out;

import com.github.matsik.dto.BookingPartitionKey;
import com.github.matsik.dto.TimeRange;

import java.util.UUID;

public interface CreateBookingPort {

    long findOverlappingBookingCount(BookingPartitionKey bookingPartitionKey, TimeRange timeRange);

    UUID createBooking(BookingPartitionKey bookingPartitionKey, UUID userId, TimeRange timeRange);
}
