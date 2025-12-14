package com.github.matsik.query.application.domin;

import com.github.matsik.dto.TimeRange;
import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Builder
public record UserBooking(
        UUID serviceId,
        LocalDate date,
        UUID bookingId,
        TimeRange timeRange
) {
}
