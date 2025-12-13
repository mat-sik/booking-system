package com.github.matsik.command.application.port.out;

import com.github.matsik.dto.BookingPartitionKey;

import java.util.Optional;
import java.util.UUID;

public interface DeleteBookingPort {

    Optional<UUID> findBookingOwner(BookingPartitionKey bookingPartitionKey, UUID bookingId);

    void deleteBooking(BookingPartitionKey bookingPartitionKey, UUID userId, UUID bookingId);
}
