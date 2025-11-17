package com.github.matsik.query.booking.service;

import com.github.matsik.dto.BookingPartitionKey;
import com.github.matsik.query.booking.query.GetAvailableTimeRangesQuery;
import com.github.matsik.query.booking.query.GetFirstUserBookingsQuery;
import com.github.matsik.query.booking.query.GetNextUserBookingsQuery;
import com.github.matsik.query.booking.query.GetUserBookingQuery;
import com.github.matsik.query.booking.query.GetUserBookingsQuery;
import com.github.matsik.query.booking.repository.BookingRepository;
import com.github.matsik.dto.TimeRange;
import com.github.matsik.query.booking.repository.projection.UserBooking;
import com.github.matsik.query.booking.service.exception.UserBookingNotFoundException;
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
        BookingPartitionKey key = query.bookingPartitionKey();
        List<TimeRange> unavailableTimeRanges = repository.getBookedTimeRanges(key.serviceId(), key.date());

        int serviceDuration = availableTimeRangesCalculator.getSystemServiceDuration(query.serviceDuration());

        return availableTimeRangesCalculator.getAvailableTimeRanges(unavailableTimeRanges, serviceDuration);
    }

    @WithSpan(kind = SpanKind.SERVER)
    public TimeRange getUserBookingTimeRange(GetUserBookingQuery query) {
        BookingPartitionKey key = query.bookingPartitionKey();

        return repository.getUserBookingTimeRange(query.userId(), key.serviceId(), key.date(), query.bookingId())
                .orElseThrow(() -> new UserBookingNotFoundException(query));
    }

    public List<UserBooking> getUserBookings(GetUserBookingsQuery query) {
        return switch (query) {
            case GetFirstUserBookingsQuery first -> getFirstUserBookings(first);
            case GetNextUserBookingsQuery next -> getNextUserBookings(next);
        };
    }

    @WithSpan(kind = SpanKind.SERVER)
    private List<UserBooking> getFirstUserBookings(GetFirstUserBookingsQuery query) {
        return repository.getFirstUserBookings(query.userId(), query.limit());
    }

    @WithSpan(kind = SpanKind.SERVER)
    private List<UserBooking> getNextUserBookings(GetNextUserBookingsQuery query) {
        return repository.getNextUserBookings(
                query.userId(),
                query.cursorServiceId(),
                query.cursorDate(),
                query.cursorBookingId(),
                query.limit()
        );
    }

}
