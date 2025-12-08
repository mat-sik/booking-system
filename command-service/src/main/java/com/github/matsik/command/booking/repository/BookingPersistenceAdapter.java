package com.github.matsik.command.booking.repository;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.DefaultBatchType;
import com.datastax.oss.driver.api.core.cql.Row;
import com.github.matsik.cassandra.entity.BookingByServiceAndDate;
import com.github.matsik.cassandra.entity.BookingByUser;
import com.github.matsik.command.booking.service.BookingCommandsPort;
import com.github.matsik.dto.BookingPartitionKey;
import com.github.matsik.dto.TimeRange;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BookingPersistenceAdapter implements BookingCommandsPort {

    private final CqlSession session;
    private final BookingRepository bookingRepository;

    @Override
    @WithSpan(kind = SpanKind.CONSUMER)
    public UUID createBooking(UUID serviceId, LocalDate date, UUID userId, int start, int end) {
        UUID bookingId = UUID.randomUUID();

        BookingByServiceAndDate bookingByServiceAndDate = BookingByServiceAndDate.builder()
                .serviceId(serviceId)
                .date(date)
                .bookingId(bookingId)
                .start(start)
                .end(end)
                .userId(userId)
                .build();

        BoundStatement createBookingByServiceAndDate = bookingRepository.save(bookingByServiceAndDate);

        BookingByUser bookingByUser = BookingByUser.builder()
                .userId(userId)
                .serviceId(serviceId)
                .date(date)
                .bookingId(bookingId)
                .start(start)
                .end(end)
                .build();

        BoundStatement createBookingByUser = bookingRepository.save(bookingByUser);

        BatchStatement batchStatement = BatchStatement.builder(DefaultBatchType.LOGGED)
                .addStatement(createBookingByServiceAndDate)
                .addStatement(createBookingByUser)
                .build();

        session.execute(batchStatement);

        return bookingId;
    }

    @Override
    @WithSpan(kind = SpanKind.CONSUMER)
    public void deleteBooking(UUID serviceId, LocalDate date, UUID userId, UUID bookingId) {
        BoundStatement deleteBookingByServiceAndDate = bookingRepository.deleteByPrimaryKey(serviceId, date, bookingId);

        BoundStatement deleteBookingByUser = bookingRepository.deleteByPrimaryKey(userId, serviceId, date, bookingId);

        BatchStatement batchStatement = BatchStatement.builder(DefaultBatchType.LOGGED)
                .addStatement(deleteBookingByServiceAndDate)
                .addStatement(deleteBookingByUser)
                .build();

        session.execute(batchStatement);
    }

    @Override
    public Optional<UUID> findBookingOwner(UUID serviceId, LocalDate date, UUID bookingId) {
        Row row = bookingRepository.findBookingOwner(serviceId, date, bookingId);
        return Optional.ofNullable(row)
                .map(rowValue -> rowValue.getUuid("user_id"));
    }

    @Override
    public long findOverlappingBookingCount(UUID serviceId, LocalDate date, int start, int end) {
        return bookingRepository.findOverlappingBookingCount(serviceId, date, start, end);
    }

}
