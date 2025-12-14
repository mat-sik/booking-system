package com.github.matsik.query.adapter.out;

import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.github.matsik.dto.TimeRange;
import com.github.matsik.query.application.domin.UserBooking;
import com.github.matsik.query.application.port.out.GetBookedTimeRangesPort;
import com.github.matsik.query.application.port.out.GetFirstUserBookingsPort;
import com.github.matsik.query.application.port.out.GetNextUserBookingsPort;
import com.github.matsik.query.application.port.out.GetUserBookingTimeRangePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class BookingPersistenceAdapter implements GetUserBookingTimeRangePort, GetBookedTimeRangesPort, GetFirstUserBookingsPort, GetNextUserBookingsPort {

    private final BookingRepository bookingRepository;

    public Optional<TimeRange> getUserBookingTimeRange(UUID userId, UUID serviceId, LocalDate date, UUID bookingId) {
        Row row = bookingRepository.getUserBookingTimeRange(userId, serviceId, date, bookingId);
        return Optional.ofNullable(row)
                .map(TimeRange::of);
    }

    public List<TimeRange> getBookedTimeRanges(UUID serviceId, LocalDate date) {
        return bookingRepository.getBookedTimeRanges(serviceId, date)
                .map(TimeRange::of)
                .all()
                .stream()
                .toList();
    }

    public List<UserBooking> getFirstUserBookings(UUID userId, int limit) {
        ResultSet resultSet = bookingRepository.getFirstUserBookings(userId, limit);
        return resultSet
                .map(UserBookingMapper::from)
                .all()
                .stream()
                .toList();
    }

    public List<UserBooking> getNextUserBookings(
            UUID userId,
            UUID cursorServiceId,
            LocalDate cursorDate,
            UUID cursorBookingId,
            int limit
    ) {
        ResultSet resultSet = bookingRepository.getNextUserBookings(userId, cursorServiceId, cursorDate, cursorBookingId, limit);
        return resultSet
                .map(UserBookingMapper::from)
                .all()
                .stream()
                .toList();
    }
}
