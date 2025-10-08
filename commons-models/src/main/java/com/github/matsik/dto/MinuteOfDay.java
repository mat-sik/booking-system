package com.github.matsik.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@EqualsAndHashCode
public class MinuteOfDay {

    private static final int START = 0;
    private static final int END = 24 * 60;

    private final int minuteOfDay;

    private MinuteOfDay(int minuteOfDay) {
        if (minuteOfDay < START || minuteOfDay > END) {
            throw new IllegalArgumentException(String.format("minuteOfDay must be between %d and %d", START, END));
        }
        this.minuteOfDay = minuteOfDay;
    }

    public static MinuteOfDay of(int minuteOfDay) {
        return new MinuteOfDay(minuteOfDay);
    }

    public boolean isBefore(MinuteOfDay other) {
        return minuteOfDay < other.minuteOfDay;
    }

    public boolean isAfter(MinuteOfDay other) {
        return minuteOfDay > other.minuteOfDay;
    }

}
