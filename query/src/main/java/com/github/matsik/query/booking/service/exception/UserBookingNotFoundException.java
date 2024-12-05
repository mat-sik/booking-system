package com.github.matsik.query.booking.service.exception;

import com.github.matsik.request.query.GetBookingQuery;

public class UserBookingNotFoundException extends RuntimeException {
    public UserBookingNotFoundException(GetBookingQuery query) {
        super(String.format("UserBooking(date: %s, serviceId: %s, bookingId: %s) was not found.",
                query.serviceBookingIdentifier().date(),
                query.serviceBookingIdentifier().serviceId(),
                query.bookingId())
        );
    }
}
