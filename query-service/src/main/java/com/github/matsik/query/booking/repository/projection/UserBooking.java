package com.github.matsik.query.booking.repository.projection;

import com.datastax.oss.driver.api.core.cql.Row;
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
    public static UserBooking of(Row row) {
        return UserBooking.builder()
                .serviceId(row.getUuid("service_id"))
                .date(row.getLocalDate("date"))
                .bookingId(row.getUuid("booking_id"))
                .timeRange(TimeRange.of(
                        row.getInt("start"),
                        row.getInt("end")
                ))
                .build();
    }
}
