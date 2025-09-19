package com.github.matsik.command.booking.repository;

import com.github.matsik.cassandra.model.Booking;
import com.github.matsik.cassandra.model.BookingKey;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;

import java.time.LocalDate;
import java.util.UUID;

public interface BookingRepository extends CassandraRepository<Booking, BookingKey> {

    @Query("""
        SELECT COUNT(*)
        FROM bookings
        WHERE service_id = :serviceId
          AND date = :date
          AND start < :newEnd
          AND end > :newStart
        """)
    int findOverlappingBookingCount(UUID serviceId, LocalDate date, int newStart, int newEnd);
}
