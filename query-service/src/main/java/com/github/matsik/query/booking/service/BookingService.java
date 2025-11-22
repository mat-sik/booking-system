package com.github.matsik.query.booking.service;

import com.github.matsik.dto.BookingPartitionKey;
import com.github.matsik.dto.TimeRange;
import com.github.matsik.query.booking.query.GetAvailableTimeRangesQuery;
import com.github.matsik.query.booking.query.GetFirstUserBookingsQuery;
import com.github.matsik.query.booking.query.GetNextUserBookingsQuery;
import com.github.matsik.query.booking.query.GetUserBookingQuery;
import com.github.matsik.query.booking.query.GetUserBookingsQuery;
import com.github.matsik.query.booking.repository.BookingRepository;
import com.github.matsik.query.booking.repository.projection.UserBooking;
import com.github.matsik.query.booking.service.exception.UserBookingNotFoundException;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository repository;
    private final AvailableTimeRangesCalculator availableTimeRangesCalculator;

    @WithSpan(kind = SpanKind.SERVER)
    public List<TimeRange> getAvailableTimeRanges(GetAvailableTimeRangesQuery query) {
        Span span = Span.current();
        setSpanAttributes(span, query);

        BookingPartitionKey key = query.bookingPartitionKey();
        List<TimeRange> unavailableTimeRanges = repository.getBookedTimeRanges(key.serviceId(), key.date());

        int serviceDuration = availableTimeRangesCalculator.getSystemServiceDuration(query.serviceDuration());

        return availableTimeRangesCalculator.getAvailableTimeRanges(unavailableTimeRanges, serviceDuration);
    }

    private void setSpanAttributes(Span span, GetAvailableTimeRangesQuery query) {
        BookingPartitionKey bookingPartitionKey = query.bookingPartitionKey();

        span.setAttribute(AttributeKey.stringKey("get_available_time_ranges_query.booking_partition_key.service_id"), bookingPartitionKey.serviceId().toString());
        span.setAttribute(AttributeKey.stringKey("get_available_time_ranges_query.booking_partition_key.date"), bookingPartitionKey.date().toString());
        span.setAttribute(AttributeKey.longKey("get_available_time_ranges_query.service_duration"), query.serviceDuration());
    }

    @WithSpan(kind = SpanKind.SERVER)
    public TimeRange getUserBookingTimeRange(GetUserBookingQuery query) {
        Span span = Span.current();
        setSpanAttributes(span, query);

        BookingPartitionKey key = query.bookingPartitionKey();

        return repository.getUserBookingTimeRange(query.userId(), key.serviceId(), key.date(), query.bookingId())
                .orElseThrow(() -> new UserBookingNotFoundException(query));
    }

    private void setSpanAttributes(Span span, GetUserBookingQuery query) {
        BookingPartitionKey bookingPartitionKey = query.bookingPartitionKey();

        span.setAttribute(AttributeKey.stringKey("get_user_booking_query.booking_partition_key.service_id"), bookingPartitionKey.serviceId().toString());
        span.setAttribute(AttributeKey.stringKey("get_user_booking_query.booking_partition_key.date"), bookingPartitionKey.date().toString());
        span.setAttribute(AttributeKey.stringKey("get_user_booking_query.user_id"), query.userId().toString());
        span.setAttribute(AttributeKey.stringKey("get_user_booking_query.booking_id"), query.bookingId().toString());
    }

    public List<UserBooking> getUserBookings(GetUserBookingsQuery query) {
        return switch (query) {
            case GetFirstUserBookingsQuery first -> getFirstUserBookings(first);
            case GetNextUserBookingsQuery next -> getNextUserBookings(next);
        };
    }

    @WithSpan(kind = SpanKind.SERVER)
    private List<UserBooking> getFirstUserBookings(GetFirstUserBookingsQuery query) {
        Span span = Span.current();
        setSpanAttributes(span, query);

        return repository.getFirstUserBookings(query.userId(), query.limit());
    }

    private void setSpanAttributes(Span span, GetFirstUserBookingsQuery query) {
        span.setAttribute(AttributeKey.stringKey("get_first_user_bookings_query.user_id"), query.userId().toString());
        span.setAttribute(AttributeKey.longKey("get_first_user_bookings_query.limit"), query.limit());
    }

    @WithSpan(kind = SpanKind.SERVER)
    private List<UserBooking> getNextUserBookings(GetNextUserBookingsQuery query) {
        Span span = Span.current();
        setSpanAttributes(span, query);

        return repository.getNextUserBookings(
                query.userId(),
                query.cursorServiceId(),
                query.cursorDate(),
                query.cursorBookingId(),
                query.limit()
        );
    }

    private void setSpanAttributes(Span span, GetNextUserBookingsQuery query) {
        span.setAttribute(AttributeKey.stringKey("get_next_user_bookings_query.user_id"), query.userId().toString());
        span.setAttribute(AttributeKey.stringKey("get_next_user_bookings_query.cursor_service_id"), query.cursorServiceId().toString());
        span.setAttribute(AttributeKey.stringKey("get_next_user_bookings_query.cursor_date"), query.cursorDate().toString());
        span.setAttribute(AttributeKey.stringKey("get_next_user_bookings_query.cursor_booking_id"), query.cursorBookingId().toString());
        span.setAttribute(AttributeKey.longKey("get_next_user_bookings_query.limit"), query.limit());
    }
}
