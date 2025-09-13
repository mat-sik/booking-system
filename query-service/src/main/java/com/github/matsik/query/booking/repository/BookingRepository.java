package com.github.matsik.query.booking.repository;

import com.github.matsik.query.booking.model.ServiceBooking;
import com.github.matsik.query.booking.model.UserBooking;
import com.github.matsik.query.booking.query.GetBookingQuery;
import com.github.matsik.query.booking.query.GetBookingTimeRangesQuery;
import com.github.matsik.query.booking.query.GetBookingsQuery;
import com.github.matsik.query.booking.service.TimeRange;

import java.util.List;
import java.util.Optional;

public interface BookingRepository {
    Optional<UserBooking> getUserBooking(GetBookingQuery query);

    List<TimeRange> getBookingTimeRanges(GetBookingTimeRangesQuery query);

    /*
     * Spring Boot data mongodb doesn't support projections that are more advanced than the most basic of use cases.
     */
    List<ServiceBooking> getBookings(GetBookingsQuery query);
}
