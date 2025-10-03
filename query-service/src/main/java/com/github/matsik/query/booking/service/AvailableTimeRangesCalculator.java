package com.github.matsik.query.booking.service;

import com.github.matsik.query.booking.repository.projection.TimeRange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AvailableTimeRangesCalculator {

    private static final int START = 0;
    private static final int END = 24 * 60;

    private static final int SKIP = 15;

    private static final int OFFSET = 15;

    private static final int SERVICE_TIME_SLICE = 30;

    public List<TimeRange> getAvailableTimeRanges(List<TimeRange> unavailableTimeRanges, int serviceDuration) {
        List<TimeRange> availableTimeRanges = new ArrayList<>();
        for (int start = START; start <= END - serviceDuration; start += SKIP) {
            TimeRange timeRange = TimeRange.Factory.create(start, start + serviceDuration);

            boolean isAvailable = true;
            for (TimeRange unavailableTimeRange : unavailableTimeRanges) {
                if (isOverlapWithOffsets(timeRange, unavailableTimeRange)) {
                    isAvailable = false;
                    break;
                }
            }

            if (isAvailable) {
                availableTimeRanges.add(timeRange);
            }
        }
        return availableTimeRanges;
    }

    private boolean isOverlapWithOffsets(TimeRange timeRangeOne, TimeRange timeRangeTwo) {
        int start = Math.max(START, timeRangeTwo.start() - OFFSET);
        int end = Math.min(END, timeRangeTwo.end() + OFFSET);

        TimeRange offsetTimeRange = TimeRange.Factory.create(start, end);

        return isOverlap(timeRangeOne, offsetTimeRange);
    }

    private boolean isOverlap(TimeRange rangeOne, TimeRange rangeTwo) {
        return rangeTwo.start() < rangeOne.end() && rangeTwo.end() > rangeOne.start();
    }

    public int getSystemServiceDuration(int rawServiceDuration) {
        return Math.ceilDiv(rawServiceDuration, SERVICE_TIME_SLICE) * SERVICE_TIME_SLICE;
    }
}
