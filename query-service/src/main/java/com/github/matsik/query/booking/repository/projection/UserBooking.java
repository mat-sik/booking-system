package com.github.matsik.query.booking.repository.projection;

import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record UserBooking(
        UUID serviceId,
        LocalDate date,
        UUID bookingId,
        int start,
        int end
) {
    public static class Factory {
        public static UserBooking create(Row row) {
            return new UserBooking(
                    row.getUuid("service_id"),
                    row.getLocalDate("date"),
                    row.getUuid("booking_id"),
                    row.getInt("start"),
                    row.getInt("end")
            );
        }

        public static List<UserBooking> create(ResultSet resultSet) {
            return resultSet.map(Factory::create)
                    .all()
                    .stream()
                    .toList();
        }
    }
}
