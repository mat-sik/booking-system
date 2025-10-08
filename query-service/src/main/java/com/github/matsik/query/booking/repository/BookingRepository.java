package com.github.matsik.query.booking.repository;

import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.mapper.annotations.Dao;
import com.datastax.oss.driver.api.mapper.annotations.Query;
import com.datastax.oss.driver.api.mapper.annotations.StatementAttributes;
import com.github.matsik.dto.TimeRange;
import com.github.matsik.query.booking.repository.projection.UserBooking;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Dao
public interface BookingRepository {

    @Query("""
            SELECT start, end
            FROM bookings_by_user
            WHERE user_id = :userId
              AND service_id = :serviceId
              AND date = :date
              AND booking_id = :bookingId
            """)
    @StatementAttributes(consistencyLevel = "QUORUM")
    Row _getUserBookingTimeRange(UUID userId, UUID serviceId, LocalDate date, UUID bookingId);

    default Optional<TimeRange> getUserBookingTimeRange(UUID userId, UUID serviceId, LocalDate date, UUID bookingId) {
        Row row = _getUserBookingTimeRange(userId, serviceId, date, bookingId);
        return Optional.ofNullable(row)
                .map(TimeRange::of);
    }

    @Query("""
            SELECT start, end
            FROM bookings_by_service_and_date
            WHERE service_id = :serviceId
              AND date = :date
            """)
    @StatementAttributes(consistencyLevel = "QUORUM")
    ResultSet _getBookedTimeRanges(UUID serviceId, LocalDate date);

    default List<TimeRange> getBookedTimeRanges(UUID serviceId, LocalDate date) {
        return _getBookedTimeRanges(serviceId, date)
                .map(TimeRange::of)
                .all()
                .stream()
                .toList();
    }

    @Query("""
            SELECT service_id, date, booking_id, start, end
            FROM bookings_by_user
            WHERE user_id = :userId
            LIMIT :size
            """)
    @StatementAttributes(consistencyLevel = "QUORUM")
    ResultSet _getFirstUserBookings(UUID userId, int size);

    default List<UserBooking> getFirstUserBookings(UUID userId, int limit) {
        return UserBooking.Factory.create(_getFirstUserBookings(userId, limit));
    }

    @Query("""
            SELECT service_id, date, booking_id, start, end
            FROM bookings_by_user
            WHERE user_id = :userId
                AND service_id = :cursorServiceId
                AND date = :cursorDate
                AND booking_id > :cursorBookingId
            LIMIT :size
            """)
    @StatementAttributes(consistencyLevel = "QUORUM")
    ResultSet _getNextUserBookings(
            UUID userId,
            UUID cursorServiceId,
            LocalDate cursorDate,
            UUID cursorBookingId,
            int size
    );

    default List<UserBooking> getNextUserBookings(
            UUID userId,
            UUID cursorServiceId,
            LocalDate cursorDate,
            UUID cursorBookingId,
            int limit
    ) {
        return UserBooking.Factory.create(_getNextUserBookings(userId, cursorServiceId, cursorDate, cursorBookingId, limit));
    }

}
