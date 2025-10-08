package com.github.matsik.dto;

import com.datastax.oss.driver.api.core.cql.Row;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Objects;

@Getter
@Accessors(fluent = true)
@EqualsAndHashCode
public class TimeRange {

    private final MinuteOfDay start;
    private final MinuteOfDay end;

    private TimeRange(MinuteOfDay start, MinuteOfDay end) {
        if (Objects.equals(start, end)) {
            throw new IllegalArgumentException("start and end must not be equal");
        }
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("start must not be greater than end");
        }
        this.start = start;
        this.end = end;
    }

    private TimeRange(int start, int end) {
        this(MinuteOfDay.of(start), MinuteOfDay.of(end));
    }

    public static TimeRange of(Row row) {
        int start = row.getInt("start");
        int end = row.getInt("end");

        return new TimeRange(start, end);
    }

    public static TimeRange of(MinuteOfDay start, MinuteOfDay end) {
        return new TimeRange(start, end);
    }

    public static TimeRange of(int start, int end) {
        return new TimeRange(start, end);
    }

    public boolean isOverlap(TimeRange other) {
        return other.start().isBefore(end) && other.end().isAfter(start);
    }

}
