package com.github.matsik.query.application.port.out;

import com.github.matsik.query.application.domin.UserBooking;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface GetNextUserBookingsPort {

    List<UserBooking> getNextUserBookings(
            UUID userId,
            UUID cursorServiceId,
            LocalDate cursorDate,
            UUID cursorBookingId,
            int limit
    );
}
