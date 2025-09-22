package com.github.matsik.cassandra.model;

import com.datastax.oss.driver.api.mapper.annotations.ClusteringColumn;
import com.datastax.oss.driver.api.mapper.annotations.CqlName;
import com.datastax.oss.driver.api.mapper.annotations.Entity;
import com.datastax.oss.driver.api.mapper.annotations.PartitionKey;
import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Entity(defaultKeyspace = "booking_system")
@CqlName("bookings")
@Builder
public record Booking(

        @PartitionKey
        UUID serviceId,

        @PartitionKey(1)
        LocalDate date,

        @ClusteringColumn(2)
        UUID bookingId,

        UUID userId,

        int start,

        int end
) {
}
