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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BookingPersistenceAdapter implements BookingCommandsPort {

    private final CqlSession session;
    private final BookingRepository bookingRepository;

    @Override
    @WithSpan(kind = SpanKind.CONSUMER)
    public UUID createBooking(BookingPartitionKey bookingPartitionKey, UUID userId, TimeRange timeRange) {
        UUID bookingId = UUID.randomUUID();

        BookingByServiceAndDate bookingByServiceAndDate = BookingByServiceAndDate.builder()
                .serviceId(bookingPartitionKey.serviceId())
                .date(bookingPartitionKey.date())
                .bookingId(bookingId)
                .start(timeRange.start().minuteOfDay())
                .end(timeRange.end().minuteOfDay())
                .userId(userId)
                .build();

        BoundStatement createBookingByServiceAndDate = bookingRepository.save(bookingByServiceAndDate);

        BookingByUser bookingByUser = BookingByUser.builder()
                .userId(userId)
                .serviceId(bookingPartitionKey.serviceId())
                .date(bookingPartitionKey.date())
                .bookingId(bookingId)
                .start(timeRange.start().minuteOfDay())
                .end(timeRange.end().minuteOfDay())
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
    public void deleteBooking(BookingPartitionKey bookingPartitionKey, UUID userId, UUID bookingId) {
        BoundStatement deleteBookingByServiceAndDate = bookingRepository.deleteByPrimaryKey(
                bookingPartitionKey.serviceId(),
                bookingPartitionKey.date(),
                bookingId
        );

        BoundStatement deleteBookingByUser = bookingRepository.deleteByPrimaryKey(
                userId,
                bookingPartitionKey.serviceId(),
                bookingPartitionKey.date(),
                bookingId
        );

        BatchStatement batchStatement = BatchStatement.builder(DefaultBatchType.LOGGED)
                .addStatement(deleteBookingByServiceAndDate)
                .addStatement(deleteBookingByUser)
                .build();

        session.execute(batchStatement);
    }

    @Override
    public Optional<UUID> findBookingOwner(BookingPartitionKey bookingPartitionKey, UUID bookingId) {
        Row row = bookingRepository.findBookingOwner(
                bookingPartitionKey.serviceId(),
                bookingPartitionKey.date(),
                bookingId
        );
        return Optional.ofNullable(row)
                .map(rowValue -> rowValue.getUuid("user_id"));
    }

    @Override
    public long findOverlappingBookingCount(BookingPartitionKey bookingPartitionKey, TimeRange timeRange) {
        return bookingRepository.findOverlappingBookingCount(
                bookingPartitionKey.serviceId(),
                bookingPartitionKey.date(),
                timeRange.start().minuteOfDay(),
                timeRange.end().minuteOfDay()
        );
    }

    List<BookingByServiceAndDate> findAllByServiceAndDate(UUID serviceId, LocalDate date) {
        return bookingRepository.findAllByServiceAndDate(serviceId, date).all();
    }

}
