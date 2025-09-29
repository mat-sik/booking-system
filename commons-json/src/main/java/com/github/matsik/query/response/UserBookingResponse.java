package com.github.matsik.query.response;

import java.time.LocalDate;
import java.util.UUID;

public record UserBookingResponse(
        UUID serviceId,
        LocalDate date,
        UUID bookingId,
        int start,
        int end
) {
}
