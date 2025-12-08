package com.github.matsik.command.booking.service;

import com.github.matsik.dto.BookingPartitionKey;
import com.github.matsik.dto.TimeRange;

import java.util.Optional;
import java.util.UUID;

public interface BookingCommandsPort {

    long findOverlappingBookingCount(BookingPartitionKey bookingPartitionKey, TimeRange timeRange);

    UUID createBooking(BookingPartitionKey bookingPartitionKey, UUID userId, TimeRange timeRange);

    Optional<UUID> findBookingOwner(BookingPartitionKey bookingPartitionKey, UUID bookingId);

    void deleteBooking(BookingPartitionKey bookingPartitionKey, UUID userId, UUID bookingId);
}
