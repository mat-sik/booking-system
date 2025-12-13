package com.github.matsik.command.application.domain;

import com.github.matsik.command.application.port.in.DeleteBookingCommand;
import com.github.matsik.command.application.port.in.DeleteBookingUseCase;
import com.github.matsik.command.application.port.out.DeleteBookingPort;
import com.github.matsik.dto.BookingPartitionKey;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class DeleteBookingService implements DeleteBookingUseCase {

    private final DeleteBookingPort deleteBookingPort;

    @WithSpan(kind = SpanKind.CONSUMER)
    public void deleteBooking(DeleteBookingCommand command) {
        Span span = Span.current();
        setSpanAttributes(span, command);

        BookingPartitionKey bookingPartitionKey = command.bookingPartitionKey();

        Optional<UUID> ownerId = deleteBookingPort.findBookingOwner(bookingPartitionKey, command.bookingId());

        if (ownerId.isEmpty() || !Objects.equals(ownerId.get(), command.userId())) {
            String ownerIdString = ownerId.isPresent() ? ownerId.get().toString() : "";
            addSpanEventNotMatchingOwner(span, ownerIdString, command.userId().toString());
            return;
        }

        deleteBookingPort.deleteBooking(bookingPartitionKey, command.userId(), command.bookingId());
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

}
