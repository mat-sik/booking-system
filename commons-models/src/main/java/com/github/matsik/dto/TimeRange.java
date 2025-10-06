package com.github.matsik.dto;

import com.datastax.oss.driver.api.core.cql.Row;

public record TimeRange(int start, int end) {
    public static class Factory {
        public static TimeRange create(Row row) {
            return new TimeRange(row.getInt("start"), row.getInt("end"));
        }

        public static TimeRange create(int start, int end) {
            return new TimeRange(start, end);
        }
    }
}
