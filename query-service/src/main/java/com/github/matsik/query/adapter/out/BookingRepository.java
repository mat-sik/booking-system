package com.github.matsik.query.adapter.out;

import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.mapper.annotations.Dao;
import com.datastax.oss.driver.api.mapper.annotations.Query;
import com.datastax.oss.driver.api.mapper.annotations.StatementAttributes;

import java.time.LocalDate;
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
    Row getUserBookingTimeRange(UUID userId, UUID serviceId, LocalDate date, UUID bookingId);

    @Query("""
            SELECT start, end
            FROM bookings_by_service_and_date
            WHERE service_id = :serviceId
              AND date = :date
            """)
    @StatementAttributes(consistencyLevel = "QUORUM")
    ResultSet getBookedTimeRanges(UUID serviceId, LocalDate date);

    @Query("""
            SELECT service_id, date, booking_id, start, end
            FROM bookings_by_user
            WHERE user_id = :userId
            LIMIT :size
            """)
    @StatementAttributes(consistencyLevel = "QUORUM")
    ResultSet getFirstUserBookings(UUID userId, int size);

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
    ResultSet getNextUserBookings(
            UUID userId,
            UUID cursorServiceId,
            LocalDate cursorDate,
            UUID cursorBookingId,
            int size
    );
}
