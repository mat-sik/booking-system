package com.github.matsik.command.booking.service;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.DefaultBatchType;
import com.github.matsik.cassandra.entity.BookingByServiceAndDate;
import com.github.matsik.cassandra.entity.BookingByUser;
import com.github.matsik.command.booking.command.CreateBookingCommand;
import com.github.matsik.command.booking.command.DeleteBookingCommand;
import com.github.matsik.command.booking.repository.BookingRepository;
import com.github.matsik.dto.BookingPartitionKey;
import com.github.matsik.dto.TimeRange;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final CqlSession session;
    private final BookingRepository bookingRepository;

    @WithSpan(kind = SpanKind.CONSUMER)
    public void deleteBooking(DeleteBookingCommand command) {
        BookingPartitionKey bookingPartitionKey = command.bookingPartitionKey();

        Optional<UUID> ownerId = bookingRepository.findBookingOwner(
                bookingPartitionKey.serviceId(),
                bookingPartitionKey.date(),
                command.bookingId()
        );
        if (ownerId.isEmpty() || !Objects.equals(ownerId.get(), command.userId())) {
            Span.current().addEvent("Not matching owner", Attributes.of(
                    AttributeKey.stringKey("booking.owner.real"), ownerId.isPresent() ? ownerId.get().toString() : "",
                    AttributeKey.stringKey("booking.owner.provided"), command.userId().toString()
            ));
            return;
        }
        batchRemove(command);
    }

    @WithSpan(kind = SpanKind.CONSUMER)
    private void batchRemove(DeleteBookingCommand command) {
        BookingPartitionKey bookingPartitionKey = command.bookingPartitionKey();

        BoundStatement deleteBookingByServiceAndDate = bookingRepository.deleteByPrimaryKey(
                bookingPartitionKey.serviceId(),
                bookingPartitionKey.date(),
                command.bookingId()
        );

        BoundStatement deleteBookingByUser = bookingRepository.deleteByPrimaryKey(
                command.userId(),
                bookingPartitionKey.serviceId(),
                bookingPartitionKey.date(),
                command.bookingId()
        );

        BatchStatement batchStatement = BatchStatement.builder(DefaultBatchType.LOGGED)
                .addStatement(deleteBookingByServiceAndDate)
                .addStatement(deleteBookingByUser)
                .build();

        session.execute(batchStatement);
    }

    @WithSpan(kind = SpanKind.CONSUMER)
    public Optional<UUID> createBooking(CreateBookingCommand command) {
        BookingPartitionKey bookingPartitionKey = command.bookingPartitionKey();
        TimeRange timeRange = command.timeRange();

        long overlappingBookingCount = findOverlappingBookings(bookingPartitionKey, timeRange);
        if (overlappingBookingCount > 0) {
            Span.current().addEvent("Booking overlap", Attributes.of(
                    AttributeKey.longKey("booking.overlap.count"), overlappingBookingCount
            ));
            return Optional.empty();
        }
        return Optional.of(batchCreate(command));
    }

    @WithSpan(kind = SpanKind.CONSUMER)
    private UUID batchCreate(CreateBookingCommand command) {
        BookingPartitionKey bookingPartitionKey = command.bookingPartitionKey();
        TimeRange timeRange = command.timeRange();

        UUID bookingId = UUID.randomUUID();

        BookingByServiceAndDate bookingByServiceAndDate = BookingByServiceAndDate.builder()
                .serviceId(bookingPartitionKey.serviceId())
                .date(bookingPartitionKey.date())
                .bookingId(bookingId)
                .start(timeRange.start().minuteOfDay())
                .end(timeRange.end().minuteOfDay())
                .userId(command.userId())
                .build();

        BoundStatement createBookingByServiceAndDate = bookingRepository.save(bookingByServiceAndDate);

        BookingByUser bookingByUser = BookingByUser.builder()
                .userId(command.userId())
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

    private long findOverlappingBookings(BookingPartitionKey bookingPartitionKey, TimeRange timeRange) {
        return bookingRepository.findOverlappingBookingCount(
                bookingPartitionKey.serviceId(),
                bookingPartitionKey.date(),
                timeRange.start().minuteOfDay(),
                timeRange.end().minuteOfDay()
        );
    }
}
