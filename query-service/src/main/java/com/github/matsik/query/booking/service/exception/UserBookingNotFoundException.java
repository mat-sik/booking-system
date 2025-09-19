package com.github.matsik.query.booking.service.exception;

import com.github.matsik.query.booking.query.GetBookingQuery;

public class UserBookingNotFoundException extends RuntimeException {
    public UserBookingNotFoundException(GetBookingQuery query) {
        super(String.format("UserBooking(date: %s, serviceId: %s, bookingId: %s) was not found.",
                query.bookingPartitionKey().date(),
                query.bookingPartitionKey().serviceId(),
                query.bookingId())
        );
    }
}
