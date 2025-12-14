package com.github.matsik.query.application.domain;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BatchType;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.github.matsik.cassandra.entity.BookingByServiceAndDate;
import com.github.matsik.cassandra.entity.BookingByUser;
import com.github.matsik.query.CassandraAdapterUtils;
import com.github.matsik.query.CassandraContainerTestBase;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;

@RequiredArgsConstructor
abstract class CassandraBookingUseCaseTestBase extends CassandraContainerTestBase {

    private final CqlSession cqlSession;

    @AfterEach
    protected void afterEach() {
        clearBookingsTable();
    }

    protected void clearBookingsTable() {
        cqlSession.execute("TRUNCATE booking_system.bookings_by_service_and_date");
        cqlSession.execute("TRUNCATE booking_system.bookings_by_user");
    }

    protected void persistBooking(CassandraAdapterUtils.Booking booking) {
        BookingByServiceAndDate bookingByServiceAndDate = booking.bookingByServiceAndDate();

        BoundStatement insertBookingServiceAndDate = cqlSession.prepare(
                "INSERT INTO booking_system.bookings_by_service_and_date " +
                        "(service_id, date, booking_id, user_id, start, end) " +
                        "VALUES (?, ?, ?, ?, ?, ?)"
        ).bind(
                bookingByServiceAndDate.serviceId(),
                bookingByServiceAndDate.date(),
                bookingByServiceAndDate.bookingId(),
                bookingByServiceAndDate.userId(),
                bookingByServiceAndDate.start(),
                bookingByServiceAndDate.end()
        );

        BookingByUser insertBookingByUser = booking.bookingByUser();
        BoundStatement insertUser = cqlSession.prepare(
                "INSERT INTO booking_system.bookings_by_user " +
                        "(user_id, service_id, date, booking_id, start, end) " +
                        "VALUES (?, ?, ?, ?, ?, ?)"
        ).bind(
                insertBookingByUser.userId(),
                insertBookingByUser.serviceId(),
                insertBookingByUser.date(),
                insertBookingByUser.bookingId(),
                insertBookingByUser.start(),
                insertBookingByUser.end()
        );

        BatchStatement batch = BatchStatement.builder(BatchType.LOGGED)
                .addStatement(insertBookingServiceAndDate)
                .addStatement(insertUser)
                .build();

        cqlSession.execute(batch);
    }
}
