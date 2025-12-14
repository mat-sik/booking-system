package com.github.matsik.query.booking.application.port.in;

import com.github.matsik.dto.TimeRange;

import java.util.Optional;

public interface GetUserBookingUseCase {

    Optional<TimeRange> getUserBookingTimeRange(GetUserBookingQuery query);
}
