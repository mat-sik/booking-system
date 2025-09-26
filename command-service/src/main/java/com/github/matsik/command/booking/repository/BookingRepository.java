package com.github.matsik.command.booking.repository;

import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.mapper.annotations.Dao;
import com.datastax.oss.driver.api.mapper.annotations.Delete;
import com.datastax.oss.driver.api.mapper.annotations.Insert;
import com.datastax.oss.driver.api.mapper.annotations.Query;
import com.datastax.oss.driver.api.mapper.annotations.StatementAttributes;
import com.github.matsik.cassandra.model.BookingByServiceAndDate;
import com.github.matsik.cassandra.model.BookingByUser;

import java.time.LocalDate;
import java.util.UUID;

@Dao
public interface BookingRepository {

    @Insert
    @StatementAttributes(consistencyLevel = "QUORUM")
    BoundStatement save(BookingByServiceAndDate booking);

    @Insert
    @StatementAttributes(consistencyLevel = "QUORUM")
    BoundStatement save(BookingByUser booking);

    @Delete(entityClass = BookingByServiceAndDate.class)
    @StatementAttributes(consistencyLevel = "QUORUM")
    BoundStatement deleteByPrimaryKey(UUID serviceId, LocalDate date, UUID bookingId);

    @Delete(entityClass = BookingByUser.class)
    @StatementAttributes(consistencyLevel = "QUORUM")
    BoundStatement deleteByPrimaryKey(UUID userId, UUID serviceId, LocalDate date, UUID bookingId);

    @Query("""
        SELECT COUNT(*)
        FROM bookings_by_service_and_date
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
