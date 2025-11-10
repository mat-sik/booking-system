package com.github.matsik.booking.client.query.exception;

import java.time.LocalDate;
import java.util.UUID;

public class UserBookingNotFoundException extends RuntimeException {
    public UserBookingNotFoundException(UUID serviceId, LocalDate date, UUID userId, UUID bookingId) {
        super(String.format("UserBooking(serviceId: %s, date %s, userId: %s, bookingId: %s) was not found.",
                serviceId.toString(),
                date,
                userId.toString(),
                bookingId.toString()
        ));
    }
}
