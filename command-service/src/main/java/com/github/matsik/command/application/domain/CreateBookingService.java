package com.github.matsik.command.application.domain;

import com.github.matsik.command.application.port.in.CreateBookingCommand;
import com.github.matsik.command.application.port.in.CreateBookingUseCase;
import com.github.matsik.command.application.port.out.CreateBookingPort;
import com.github.matsik.dto.BookingPartitionKey;
import com.github.matsik.dto.TimeRange;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.UUID;


@RequiredArgsConstructor
public class CreateBookingService implements CreateBookingUseCase {

    private final CreateBookingPort createBookingPort;

    @WithSpan(kind = SpanKind.CONSUMER)
    public Optional<UUID> createBooking(CreateBookingCommand command) {
        Span span = Span.current();
        setSpanAttributes(span, command);

        BookingPartitionKey bookingPartitionKey = command.bookingPartitionKey();
        TimeRange timeRange = command.timeRange();

        long overlappingBookingCount = createBookingPort.findOverlappingBookingCount(bookingPartitionKey, timeRange);

        if (overlappingBookingCount > 0) {
            addSpanEventOverlappingBookingCount(span, overlappingBookingCount);
            return Optional.empty();
        }

        UUID bookingId = createBookingPort.createBooking(bookingPartitionKey, command.userId(), timeRange);

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
