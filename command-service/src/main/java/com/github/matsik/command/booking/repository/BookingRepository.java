package com.github.matsik.command.booking.repository;

import com.datastax.oss.driver.api.core.PagingIterable;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.mapper.annotations.Dao;
import com.datastax.oss.driver.api.mapper.annotations.Delete;
import com.datastax.oss.driver.api.mapper.annotations.Insert;
import com.datastax.oss.driver.api.mapper.annotations.Query;
import com.datastax.oss.driver.api.mapper.annotations.StatementAttributes;
import com.github.matsik.cassandra.entity.BookingByServiceAndDate;
import com.github.matsik.cassandra.entity.BookingByUser;

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

    @Query("SELECT * FROM bookings_by_service_and_date WHERE service_id = :serviceId AND date = :date")
    @StatementAttributes(consistencyLevel = "QUORUM")
    PagingIterable<BookingByServiceAndDate> findAllByServiceAndDate(UUID serviceId, LocalDate date);

    @Delete(entityClass = BookingByServiceAndDate.class)
    @StatementAttributes(consistencyLevel = "QUORUM")
    BoundStatement deleteByPrimaryKey(UUID serviceId, LocalDate date, UUID bookingId);

    @Delete(entityClass = BookingByUser.class)
    @StatementAttributes(consistencyLevel = "QUORUM")
    BoundStatement deleteByPrimaryKey(UUID userId, UUID serviceId, LocalDate date, UUID bookingId);
}
