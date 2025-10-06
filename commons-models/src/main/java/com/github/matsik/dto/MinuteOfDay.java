package com.github.matsik.dto;

public record MinuteOfDay(int minuteOfDay) {

    private static final int START = 0;
    private static final int END = 24 * 60;

    public MinuteOfDay {
        if (minuteOfDay < START || minuteOfDay > END) {
            throw new IllegalArgumentException(String.format("minuteOfDay must be between %d and %d", START, END));
        }
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
