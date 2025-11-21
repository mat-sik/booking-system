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
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final CqlSession session;
    private final BookingRepository bookingRepository;
    private final Meter meter;

    public void deleteBooking(DeleteBookingCommand command) {
        recordMetrics(() -> {
            _deleteBooking(command);
            return null;
        }, "delete_booking");
    }

    @WithSpan(kind = SpanKind.CONSUMER)
    private void _deleteBooking(DeleteBookingCommand command) {
        Span span = Span.current();
        setSpanAttributes(span, command);

        BookingPartitionKey bookingPartitionKey = command.bookingPartitionKey();

        Optional<UUID> ownerId = bookingRepository.findBookingOwner(
                bookingPartitionKey.serviceId(),
                bookingPartitionKey.date(),
                command.bookingId()
        );
        if (ownerId.isEmpty() || !Objects.equals(ownerId.get(), command.userId())) {
            String ownerIdString = ownerId.isPresent() ? ownerId.get().toString() : "";
            addSpanEventNotMatchingOwner(span, ownerIdString, command.userId().toString());
            return;
        }
        batchRemove(command);
    }

    private void setSpanAttributes(Span span, DeleteBookingCommand command) {
        BookingPartitionKey bookingPartitionKey = command.bookingPartitionKey();

        span.setAttribute(AttributeKey.stringKey("deleteBookingCommand.bookingPartitionKey.serviceId"), bookingPartitionKey.serviceId().toString());
        span.setAttribute(AttributeKey.stringKey("deleteBookingCommand.bookingPartitionKey.date"), bookingPartitionKey.date().toString());
        span.setAttribute(AttributeKey.stringKey("deleteBookingCommand.bookingId"), command.bookingId().toString());
        span.setAttribute(AttributeKey.stringKey("deleteBookingCommand.userId"), command.userId().toString());
    }

    private void addSpanEventNotMatchingOwner(Span span, String ownerId, String commandUserId) {
        span.addEvent("Not matching owner", Attributes.of(
                AttributeKey.stringKey("booking.owner.real"), ownerId,
                AttributeKey.stringKey("booking.owner.provided"), commandUserId
        ));
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

    public Optional<UUID> createBooking(CreateBookingCommand command) {
        return recordMetrics(() -> _createBooking(command), "create_booking");
    }

    @WithSpan(kind = SpanKind.CONSUMER)
    private Optional<UUID> _createBooking(CreateBookingCommand command) {
        Span span = Span.current();
        setSpanAttributes(span, command);

        BookingPartitionKey bookingPartitionKey = command.bookingPartitionKey();
        TimeRange timeRange = command.timeRange();

        long overlappingBookingCount = findOverlappingBookings(bookingPartitionKey, timeRange);
        if (overlappingBookingCount > 0) {
            addSpanEventOverlappingBookingCount(span, overlappingBookingCount);
            return Optional.empty();
        }
        return Optional.of(batchCreate(command));
    }

    private void setSpanAttributes(Span span, CreateBookingCommand command) {
        BookingPartitionKey bookingPartitionKey = command.bookingPartitionKey();
        TimeRange timeRange = command.timeRange();

        span.setAttribute(AttributeKey.stringKey("createBookingCommand.bookingPartitionKey.serviceId"), bookingPartitionKey.serviceId().toString());
        span.setAttribute(AttributeKey.stringKey("createBookingCommand.bookingPartitionKey.date"), bookingPartitionKey.date().toString());
        span.setAttribute(AttributeKey.stringKey("createBookingCommand.userId"), command.userId().toString());
        span.setAttribute(AttributeKey.longKey("createBookingCommand.timeRange.end"), timeRange.start().minuteOfDay());
        span.setAttribute(AttributeKey.longKey("createBookingCommand.timeRange.start"), timeRange.start().minuteOfDay());
    }

    private void addSpanEventOverlappingBookingCount(Span span, long overlappingBookingCount) {
        span.addEvent("Booking overlap", Attributes.of(
                AttributeKey.longKey("booking.overlap.count"), overlappingBookingCount
        ));
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

    private <T> T recordMetrics(Supplier<T> operation, String commandName) {
        long startTime = System.nanoTime();
        T result = operation.get();
        long duration = System.nanoTime() - startTime;

        recordDurationAndIncrementCounter(meter, duration, commandName);

        return result;
    }

    private void recordDurationAndIncrementCounter(Meter meter, long duration, String commandName) {
        LongCounter counter = meter.counterBuilder(String.format("%s.records", commandName))
                .setDescription(String.format("Total %s records", commandName))
                .setUnit("requests")
                .build();

        DoubleHistogram histogram = meter.histogramBuilder(String.format("%s.record.processing.duration", commandName))
                .setDescription(String.format("Duration of processing %s record", commandName))
                .setUnit("ms")
                .build();

        counter.add(1L);
        histogram.record(duration);
    }
}
