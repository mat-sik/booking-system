package com.github.matsik.query.booking.adapter.out;

import com.datastax.oss.driver.api.core.cql.Row;
import com.github.matsik.dto.TimeRange;
import com.github.matsik.query.booking.application.domin.UserBooking;

class UserBookingMapper {
    static UserBooking from(Row row) {
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
