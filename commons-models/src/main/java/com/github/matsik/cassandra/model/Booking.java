package com.github.matsik.cassandra.model;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.UUID;

@Table(keyspace = "booking_system", value = "bookings")
public record Booking(

        @PrimaryKey
        BookingKey key,

        @Column("user_id")
        UUID userId,

        @Column("start")
        int start,

        @Column("end")
        int end
) {
}
