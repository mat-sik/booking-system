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

    // There should be 15 minutes between consecutive services
    private static final int SKIP = 15;

    // Duration of a service should be multiple of this value
    private static final int SERVICE_TIME_SLICE = 30;

    private final BookingRepository repository;

    public List<TimeRange> getAvailableTimeRanges(GetAvailableTimeRangesQuery query) {
        BookingPartitionKey key = query.bookingPartitionKey();
        List<TimeRange> unavailableTimeRanges = repository.getBookedTimeRanges(key.serviceId(), key.date());

        int serviceDuration = getSystemServiceDuration(query.serviceDuration());

        return getAvailableTimeRanges(unavailableTimeRanges, serviceDuration);
    }

    /*
     * The time complexity of this implementation is O(n*m), where n is the amount of all possible time ranges and m
     * is the amount of unavailable time ranges.
     *
     * I believe that Interval Tree could be used to efficiently check for overlap. When using this data structure, the
     * time complexity would be of O(min(n,m)*log(max(n,m))) plus time to build the tree, so basically O(nlogn).
     */
    private static List<TimeRange> getAvailableTimeRanges(List<TimeRange> unavailableTimeRanges, int serviceDuration) {
        serviceDuration = Math.max(SKIP, serviceDuration);

        List<TimeRange> availableTimeRanges = new ArrayList<>();
        for (int start = START; start <= END - SERVICE_TIME_SLICE; start += serviceDuration) {
            boolean isAvailable = true;
            int end = start + serviceDuration;

            for (TimeRange range : unavailableTimeRanges) {
                if (isOverlap(range, start, end)) {
                    isAvailable = false;
                    break;
                }
            }

            if (isAvailable) {
                availableTimeRanges.add(new TimeRange(start, end));
            }
        }
        return availableTimeRanges;
    }

    private static int getSystemServiceDuration(int rawServiceDuration) {
        return Math.ceilDiv(rawServiceDuration + SKIP, SERVICE_TIME_SLICE) * SERVICE_TIME_SLICE;
    }

    private static boolean isOverlap(TimeRange range, int start, int end) {
        return start < range.end() && end > range.start();
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

    public List<UserBooking> getFirstUserBookings(GetFirstUserBookingsQuery query) {
        return repository.getFirstUserBookings(query.userId(), query.limit());
    }

    public List<UserBooking> getNextUserBookings(GetNextUserBookingsQuery query) {
        return repository.getNextUserBookings(
                query.userId(),
                query.cursorServiceId(),
                query.cursorDate(),
                query.cursorBookingId(),
                query.limit()
        );
    }

}
