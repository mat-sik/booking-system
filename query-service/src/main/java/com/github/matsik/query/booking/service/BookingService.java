package com.github.matsik.query.booking.service;

import com.github.matsik.query.booking.model.BookingTimeRange;
import com.github.matsik.query.booking.model.ServiceBooking;
import com.github.matsik.query.booking.model.UserBooking;
import com.github.matsik.query.booking.query.GetAvailableTimeRangesQuery;
import com.github.matsik.query.booking.query.GetBookingQuery;
import com.github.matsik.query.booking.query.GetBookingsQuery;
import com.github.matsik.query.booking.repository.BookingRepository;
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
        List<BookingTimeRange> unavailableTimeRanges = repository.getBookingTimeRanges(query.getBookingTimeRangesQuery());

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
    private static List<TimeRange> getAvailableTimeRanges(List<BookingTimeRange> unavailableTimeRanges, int serviceDuration) {
        serviceDuration = Math.max(SKIP, serviceDuration);

        List<TimeRange> availableTimeRanges = new ArrayList<>();
        for (int start = START; start <= END - SERVICE_TIME_SLICE; start += serviceDuration) {
            boolean isAvailable = true;
            int end = start + serviceDuration;

            for (BookingTimeRange unavailableTimeRange : unavailableTimeRanges) {
                if (isOverlap(unavailableTimeRange, start, end)) {
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

    private static boolean isOverlap(BookingTimeRange bookingTimeRange, int start, int end) {
        return start < bookingTimeRange.end() && end > bookingTimeRange.start();
    }

    public UserBooking getUserBooking(GetBookingQuery query) {
        return repository.getUserBooking(query).orElseThrow(() -> new UserBookingNotFoundException(query));
    }

    public List<ServiceBooking> getBookings(GetBookingsQuery query) {
        return repository.getBookings(query);
    }

}
