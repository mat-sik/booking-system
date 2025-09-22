package com.github.matsik.command.booking.repository;

import com.datastax.oss.driver.api.mapper.annotations.Dao;
import com.datastax.oss.driver.api.mapper.annotations.Delete;
import com.datastax.oss.driver.api.mapper.annotations.Insert;
import com.datastax.oss.driver.api.mapper.annotations.Query;
import com.datastax.oss.driver.api.mapper.annotations.StatementAttributes;
import com.github.matsik.cassandra.model.Booking;

import java.time.LocalDate;
import java.util.UUID;

@Dao
public interface BookingRepository {

    @Insert
    @StatementAttributes(consistencyLevel = "QUORUM")
    void save(Booking booking);

    @Delete(entityClass = Booking.class)
    @StatementAttributes(consistencyLevel = "QUORUM")
    void deleteByPrimaryKey(UUID serviceId, LocalDate date, UUID bookingId);

    @Query("""
        SELECT COUNT(*)
        FROM bookings
        WHERE service_id = :serviceId AND date = :date
          AND start < :end
          AND end > :start
        ALLOW FILTERING
        """)
    @StatementAttributes(consistencyLevel = "QUORUM")
    long findOverlappingBookingCount(
            UUID serviceId,
            LocalDate date,
            int start,
            int end
    );
}
