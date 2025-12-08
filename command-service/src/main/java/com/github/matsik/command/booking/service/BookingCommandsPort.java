package com.github.matsik.command.booking.service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface BookingCommandsPort {

    long findOverlappingBookingCount(UUID serviceId, LocalDate date, int start, int end);

    UUID createBooking(UUID serviceId, LocalDate date, UUID userId, int start, int end);

    Optional<UUID> findBookingOwner(UUID serviceId, LocalDate date, UUID bookingId);

    void deleteBooking(UUID serviceId, LocalDate date, UUID userId, UUID bookingId);
}
