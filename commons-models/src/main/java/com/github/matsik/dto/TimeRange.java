package com.github.matsik.dto;

import com.datastax.oss.driver.api.core.cql.Row;

import java.util.Objects;

public record TimeRange(MinuteOfDay start, MinuteOfDay end) {

    public static class Factory {
        public static TimeRange create(Row row) {
            int start = row.getInt("start");
            int end = row.getInt("end");

            return new TimeRange(MinuteOfDay.of(start), MinuteOfDay.of(end));
        }

        public static TimeRange create(MinuteOfDay start, MinuteOfDay end) {
            return new TimeRange(start, end);
        }
    }

    public TimeRange {
        if (Objects.equals(start, end)) {
            throw new IllegalArgumentException("start and end must not be equal");
        }
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("start must not be greater than end");
        }
    }

    public boolean isOverlap(TimeRange other) {
        return other.start().isBefore(end) && other.end().isAfter(start);
    }

}
