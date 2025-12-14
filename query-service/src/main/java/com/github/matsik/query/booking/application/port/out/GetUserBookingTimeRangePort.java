package com.github.matsik.query.booking.application.port.out;

import com.github.matsik.dto.TimeRange;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface GetUserBookingTimeRangePort {

    Optional<TimeRange> getUserBookingTimeRange(UUID userId, UUID serviceId, LocalDate date, UUID bookingId);
}
