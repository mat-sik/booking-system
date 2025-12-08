package com.github.matsik.command.booking.service;

import com.github.matsik.dto.BookingPartitionKey;
import com.github.matsik.dto.TimeRange;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface BookingCommandsPort {

    long findOverlappingBookingCount(
            BookingPartitionKey bookingPartitionKey,
            TimeRange timeRange
    );

    UUID createBooking(
            UUID userId,
            BookingPartitionKey bookingPartitionKey,
            TimeRange timeRange
    );

    Optional<UUID> findBookingOwner(UUID serviceId, LocalDate date, UUID bookingId);

    void deleteBooking(
            UUID userId,
            BookingPartitionKey bookingPartitionKey,
            UUID bookingId);
}
