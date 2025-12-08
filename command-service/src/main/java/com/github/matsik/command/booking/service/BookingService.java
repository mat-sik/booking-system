package com.github.matsik.command.booking.service;

import com.github.matsik.command.booking.command.CreateBookingCommand;
import com.github.matsik.command.booking.command.DeleteBookingCommand;
import com.github.matsik.command.booking.repository.BookingPersistenceAdapter;
import com.github.matsik.dto.BookingPartitionKey;
import com.github.matsik.dto.TimeRange;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.github.matsik.command.metrics.MetricsRecorder.recordMetrics;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingPersistenceAdapter bookingPersistenceAdapter;

    private final LongCounter recordCounter;
    private final DoubleHistogram recordHistogram;

    public void deleteBooking(DeleteBookingCommand command) {
        recordMetrics(recordCounter, recordHistogram, () -> _deleteBooking(command), "delete_booking");
    }

    @WithSpan(kind = SpanKind.CONSUMER)
    private void _deleteBooking(DeleteBookingCommand command) {
        Span span = Span.current();
        setSpanAttributes(span, command);

        BookingPartitionKey bookingPartitionKey = command.bookingPartitionKey();

        Optional<UUID> ownerId = bookingPersistenceAdapter.findBookingOwner(
                bookingPartitionKey.serviceId(),
                bookingPartitionKey.date(),
                command.bookingId()
        );

        if (ownerId.isEmpty() || !Objects.equals(ownerId.get(), command.userId())) {
            String ownerIdString = ownerId.isPresent() ? ownerId.get().toString() : "";
            addSpanEventNotMatchingOwner(span, ownerIdString, command.userId().toString());
            return;
        }

        bookingPersistenceAdapter.batchDeleteBooking(
                command.userId(),
                bookingPartitionKey,
                command.bookingId()
        );
    }

    private void setSpanAttributes(Span span, DeleteBookingCommand command) {
        BookingPartitionKey bookingPartitionKey = command.bookingPartitionKey();

        span.setAttribute(AttributeKey.stringKey("delete_booking_command.booking_partition_key.serviceId"), bookingPartitionKey.serviceId().toString());
        span.setAttribute(AttributeKey.stringKey("delete_booking_command.booking_partition_key.date"), bookingPartitionKey.date().toString());
        span.setAttribute(AttributeKey.stringKey("delete_booking_command.bookingId"), command.bookingId().toString());
        span.setAttribute(AttributeKey.stringKey("delete_booking_command.userId"), command.userId().toString());
    }

    private void addSpanEventNotMatchingOwner(Span span, String ownerId, String commandUserId) {
        span.addEvent("Not matching owner", Attributes.of(
                AttributeKey.stringKey("booking.owner.real"), ownerId,
                AttributeKey.stringKey("booking.owner.provided"), commandUserId
        ));
    }

    public Optional<UUID> createBooking(CreateBookingCommand command) {
        return recordMetrics(recordCounter, recordHistogram, () -> _createBooking(command), "create_booking");
    }

    @WithSpan(kind = SpanKind.CONSUMER)
    private Optional<UUID> _createBooking(CreateBookingCommand command) {
        Span span = Span.current();
        setSpanAttributes(span, command);

        BookingPartitionKey bookingPartitionKey = command.bookingPartitionKey();
        TimeRange timeRange = command.timeRange();

        long overlappingBookingCount = bookingPersistenceAdapter.findOverlappingBookingCount(
                bookingPartitionKey,
                timeRange
        );

        if (overlappingBookingCount > 0) {
            addSpanEventOverlappingBookingCount(span, overlappingBookingCount);
            return Optional.empty();
        }

        UUID bookingId = bookingPersistenceAdapter.batchCreateBooking(
                command.userId(),
                bookingPartitionKey,
                timeRange
        );

        return Optional.of(bookingId);
    }

    private void setSpanAttributes(Span span, CreateBookingCommand command) {
        BookingPartitionKey bookingPartitionKey = command.bookingPartitionKey();
        TimeRange timeRange = command.timeRange();

        span.setAttribute(AttributeKey.stringKey("create_booking_command.booking_partition_key.service_id"), bookingPartitionKey.serviceId().toString());
        span.setAttribute(AttributeKey.stringKey("create_booking_command.booking_partition_key.date"), bookingPartitionKey.date().toString());
        span.setAttribute(AttributeKey.stringKey("create_booking_command.user_id"), command.userId().toString());
        span.setAttribute(AttributeKey.longKey("create_booking_command.time_range.end"), timeRange.start().minuteOfDay());
        span.setAttribute(AttributeKey.longKey("create_booking_command.time_range.start"), timeRange.start().minuteOfDay());
    }

    private void addSpanEventOverlappingBookingCount(Span span, long overlappingBookingCount) {
        span.addEvent("Booking overlap", Attributes.of(
                AttributeKey.longKey("booking.overlap.count"), overlappingBookingCount
        ));
    }
}
