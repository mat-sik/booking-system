package com.github.matsik.query.booking.query;

import java.time.LocalDate;
import java.util.UUID;

public record GetNextUserBookingsQuery(
        UUID userId,
        UUID cursorServiceId,
        LocalDate cursorDate,
        UUID cursorBookingId,
        int limit
) implements GetUserBookingsQuery {
}
