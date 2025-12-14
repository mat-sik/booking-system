package com.github.matsik.query.booking.adapter.in;

import com.github.matsik.query.booking.application.port.in.GetUserBookingQuery;

public class UserBookingNotFoundException extends RuntimeException {
    public UserBookingNotFoundException(GetUserBookingQuery query) {
        super(String.format("UserBooking(serviceId: %s, date %s, userId: %s, bookingId: %s) was not found.",
                query.bookingPartitionKey().serviceId(),
                query.bookingPartitionKey().date(),
                query.userId(),
                query.bookingId()
        ));
    }
}
