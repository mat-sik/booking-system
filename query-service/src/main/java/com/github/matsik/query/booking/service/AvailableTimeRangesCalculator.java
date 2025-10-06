package com.github.matsik.query.booking.service;

import com.github.matsik.dto.MinuteOfDay;
import com.github.matsik.dto.TimeRange;
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
            MinuteOfDay startMinuteOfDay = MinuteOfDay.of(start);
            MinuteOfDay endMinuteOfDay = MinuteOfDay.of(start + serviceDuration);
            TimeRange timeRange = TimeRange.Factory.create(startMinuteOfDay, endMinuteOfDay);

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
        int start = Math.max(START, timeRangeTwo.start().minuteOfDay() - OFFSET);
        int end = Math.min(END, timeRangeTwo.end().minuteOfDay() + OFFSET);

        TimeRange offsetTimeRange = TimeRange.Factory.create(
                MinuteOfDay.of(start),
                MinuteOfDay.of(end)
        );

        return timeRangeOne.isOverlap(offsetTimeRange);
    }

    public int getSystemServiceDuration(int rawServiceDuration) {
        return Math.ceilDiv(rawServiceDuration, SERVICE_TIME_SLICE) * SERVICE_TIME_SLICE;
    }
}
