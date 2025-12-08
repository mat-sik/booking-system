package com.github.matsik.command.booking.repository;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.DefaultBatchType;
import com.datastax.oss.driver.api.core.cql.Row;
import com.github.matsik.cassandra.entity.BookingByServiceAndDate;
import com.github.matsik.cassandra.entity.BookingByUser;
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
public class BookingPersistenceAdapter {

    private final CqlSession session;
    private final BookingRepository bookingRepository;

    @WithSpan(kind = SpanKind.CONSUMER)
    public UUID batchCreateBooking(
            UUID userId,
            BookingPartitionKey bookingPartitionKey,
            TimeRange timeRange
    ) {
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

    @WithSpan(kind = SpanKind.CONSUMER)
    public void batchDeleteBooking(
            UUID userId,
            BookingPartitionKey bookingPartitionKey,
            UUID bookingId
    ) {
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

    public Optional<UUID> findBookingOwner(UUID serviceId, LocalDate date, UUID bookingId) {
        Row row = bookingRepository._findBookingOwner(serviceId, date, bookingId);
        return Optional.ofNullable(row)
                .map(rowValue -> rowValue.getUuid("user_id"));
    }

    public long findOverlappingBookingCount(
            BookingPartitionKey bookingPartitionKey,
            TimeRange timeRange
    ) {
        return bookingRepository.findOverlappingBookingCount(
                bookingPartitionKey.serviceId(),
                bookingPartitionKey.date(),
                timeRange.start().minuteOfDay(),
                timeRange.end().minuteOfDay()
        );
    }

}
