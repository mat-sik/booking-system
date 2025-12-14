package com.github.matsik.query.booking.application.port.out;

import com.github.matsik.dto.TimeRange;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface GetBookedTimeRangesPort {

    List<TimeRange> getBookedTimeRanges(UUID serviceId, LocalDate date);
}
