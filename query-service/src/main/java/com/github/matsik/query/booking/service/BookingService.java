package com.github.matsik.query.booking.service;

import com.github.matsik.cassandra.model.BookingPartitionKey;
import com.github.matsik.query.booking.query.GetAvailableTimeRangesQuery;
import com.github.matsik.query.booking.query.GetFirstUserBookingsQuery;
import com.github.matsik.query.booking.query.GetNextUserBookingsQuery;
import com.github.matsik.query.booking.query.GetUserBookingQuery;
import com.github.matsik.query.booking.query.GetUserBookingsQuery;
import com.github.matsik.query.booking.repository.BookingRepository;
import com.github.matsik.query.booking.repository.projection.TimeRange;
import com.github.matsik.query.booking.repository.projection.UserBooking;
import com.github.matsik.query.booking.service.exception.UserBookingNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private static final int START = 0;
    private static final int END = 24 * 60;

    private static final int SKIP = 15;

    private static final int OFFSET = 15;

    private static final int SERVICE_TIME_SLICE = 30;

    private final BookingRepository repository;

    public List<TimeRange> getAvailableTimeRanges(GetAvailableTimeRangesQuery query) {
        BookingPartitionKey key = query.bookingPartitionKey();
        List<TimeRange> unavailableTimeRanges = repository.getBookedTimeRanges(key.serviceId(), key.date());

        int serviceDuration = getSystemServiceDuration(query.serviceDuration());

        return getAvailableTimeRanges(unavailableTimeRanges, serviceDuration);
    }

    private List<TimeRange> getAvailableTimeRanges(List<TimeRange> unavailableTimeRanges, int serviceDuration) {
        List<TimeRange> availableTimeRanges = new ArrayList<>();
        for (int start = START; start <= END - serviceDuration; start += SKIP) {
            TimeRange timeRange = TimeRange.Factory.create(start, start + serviceDuration);

            boolean isAvailable = true;
            for (TimeRange unavailableTimeRange : unavailableTimeRanges) {
                if (isOverlapWithOffsets(timeRange, unavailableTimeRange)) {
                    isAvailable = false;
                    break;
                }
            }

            if (isAvailable) {
                availableTimeRanges.add(timeRange);
            }
        }
        return availableTimeRanges;
    }

    private int getSystemServiceDuration(int rawServiceDuration) {
        return Math.ceilDiv(rawServiceDuration, SERVICE_TIME_SLICE) * SERVICE_TIME_SLICE;
    }

    private boolean isOverlapWithOffsets(TimeRange timeRangeOne, TimeRange timeRangeTwo) {
        int start = Math.max(START, timeRangeTwo.start() - OFFSET);
        int end = Math.min(END, timeRangeTwo.end() + OFFSET);

        TimeRange offsetTimeRange = TimeRange.Factory.create(start, end);

        return isOverlap(timeRangeOne, offsetTimeRange);
    }

    private boolean isOverlap(TimeRange rangeOne, TimeRange rangeTwo) {
        return rangeTwo.start() < rangeOne.end() && rangeTwo.end() > rangeOne.start();
    }

    public TimeRange getUserBookingTimeRange(GetUserBookingQuery query) {
        BookingPartitionKey key = query.bookingPartitionKey();

        return repository.getUserBookingTimeRange(query.userId(), key.serviceId(), key.date(), query.bookingId())
                .orElseThrow(() -> new UserBookingNotFoundException(query));
    }

    public List<UserBooking> getUserBookings(GetUserBookingsQuery query) {
        if (query instanceof GetFirstUserBookingsQuery getFirstUserBookingsQuery) {
            return getFirstUserBookings(getFirstUserBookingsQuery);
        } else if (query instanceof GetNextUserBookingsQuery getNextUserBookingsQuery) {
            return getNextUserBookings(getNextUserBookingsQuery);
        } else {
            throw new IllegalArgumentException();
        }
    }

    private List<UserBooking> getFirstUserBookings(GetFirstUserBookingsQuery query) {
        return repository.getFirstUserBookings(query.userId(), query.limit());
    }

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
