package com.github.matsik.command.application.domain;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BatchType;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.github.matsik.cassandra.entity.BookingByServiceAndDate;
import com.github.matsik.cassandra.entity.BookingByUser;
import com.github.matsik.command.CassandraContainerTestBase;
import com.github.matsik.command.adapter.out.cassandra.BookingPersistenceService;
import com.github.matsik.dto.BookingPartitionKey;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SpringBootTest(classes = CassandraAdapterConfig.class)
abstract class CassandraBookingUseCaseTestBase extends CassandraContainerTestBase {

    @Autowired
    protected CqlSession cqlSession;

    @Autowired
    protected BookingPersistenceService bookingPersistenceService;

    @Autowired
    protected LongCounter recordCounter;

    @Autowired
    protected DoubleHistogram recordHistogram;

    @AfterEach
    void tearDown() {
        clearBookingsTable();
    }

    private void clearBookingsTable() {
        cqlSession.execute("TRUNCATE booking_system.bookings_by_service_and_date");
        cqlSession.execute("TRUNCATE booking_system.bookings_by_user");
    }

    protected Optional<BookingByServiceAndDate> findBooking(CassandraAdapterUtils.Booking booking) {
        BookingByServiceAndDate bookingByServiceAndDate = booking.bookingByServiceAndDate();

        UUID serviceId = bookingByServiceAndDate.serviceId();
        LocalDate date = bookingByServiceAndDate.date();
        int start = bookingByServiceAndDate.start();
        int end = bookingByServiceAndDate.end();

        return findBookingByTimeRange(serviceId, date, start, end);
    }

    private Optional<BookingByServiceAndDate> findBookingByTimeRange(UUID serviceId, LocalDate date, int start, int end) {
        String query = """
                SELECT service_id, date, booking_id, user_id, start, end
                FROM booking_system.bookings_by_service_and_date
                WHERE service_id = ? AND date = ? AND start = ? AND end = ?
                ALLOW FILTERING
                """;

        PreparedStatement prepared = cqlSession.prepare(query);
        BoundStatement bound = prepared.bind(serviceId, date, start, end);

        ResultSet resultSet = cqlSession.execute(bound);

        List<BookingByServiceAndDate> bookings = resultSet.map(this::bookingByServiceAndDate).all();

        if (bookings.size() > 1) {
            throw new IllegalStateException("Multiple bookings found, incorrect test definition");
        }
        return bookings.isEmpty() ? Optional.empty() : Optional.of(bookings.getFirst());
    }

    protected Optional<BookingByServiceAndDate> findBookingByServiceAndDate(BookingPartitionKey key, UUID bookingId) {
        String query = """
                SELECT service_id, date, booking_id, user_id, start, end
                FROM booking_system.bookings_by_service_and_date
                WHERE service_id = ? AND date = ? AND booking_id = ?
                """;

        PreparedStatement prepared = cqlSession.prepare(query);
        BoundStatement bound = prepared.bind(key.serviceId(), key.date(), bookingId);

        ResultSet resultSet = cqlSession.execute(bound);

        return Optional.ofNullable(resultSet.map(this::bookingByServiceAndDate).one());
    }

    private BookingByServiceAndDate bookingByServiceAndDate(Row row) {
        return BookingByServiceAndDate.builder()
                .serviceId(row.getUuid("service_id"))
                .date(row.getLocalDate("date"))
                .bookingId(row.getUuid("booking_id"))
                .userId(row.getUuid("user_id"))
                .start(row.getInt("start"))
                .end(row.getInt("end"))
                .build();
    }

    protected Optional<BookingByUser> findBookingByUser(UUID userId, BookingPartitionKey key, UUID bookingId) {
        String query = """
                SELECT service_id, date, booking_id, user_id, start, end
                FROM booking_system.bookings_by_user
                WHERE user_id = ? AND service_id = ? AND date = ? AND booking_id = ?
                """;

        PreparedStatement prepared = cqlSession.prepare(query);
        BoundStatement bound = prepared.bind(userId, key.serviceId(), key.date(), bookingId);

        ResultSet resultSet = cqlSession.execute(bound);

        return Optional.ofNullable(resultSet.map(this::bookingByUser).one());
    }

    private BookingByUser bookingByUser(Row row) {
        return BookingByUser.builder()
                .serviceId(row.getUuid("service_id"))
                .date(row.getLocalDate("date"))
                .bookingId(row.getUuid("booking_id"))
                .userId(row.getUuid("user_id"))
                .start(row.getInt("start"))
                .end(row.getInt("end"))
                .build();
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
