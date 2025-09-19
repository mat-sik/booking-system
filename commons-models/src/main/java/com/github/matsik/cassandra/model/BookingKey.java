package com.github.matsik.cassandra.model;

import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

@PrimaryKeyClass
public record BookingKey(

        @PrimaryKeyColumn(name = "service_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
        UUID serviceId,

        @PrimaryKeyColumn(name = "date", ordinal = 1, type = PrimaryKeyType.PARTITIONED)
        LocalDate date,

        @PrimaryKeyColumn(name = "booking_id", ordinal = 2, type = PrimaryKeyType.CLUSTERED)
        UUID bookingId
) implements Serializable {
}
