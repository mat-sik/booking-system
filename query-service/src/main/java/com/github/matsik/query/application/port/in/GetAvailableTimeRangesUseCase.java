package com.github.matsik.query.application.port.in;

import com.github.matsik.dto.TimeRange;

import java.util.List;

public interface GetAvailableTimeRangesUseCase {

    List<TimeRange> getAvailableTimeRanges(GetAvailableTimeRangesQuery query);
}
