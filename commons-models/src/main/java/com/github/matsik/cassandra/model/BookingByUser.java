package com.github.matsik.cassandra.model;

import com.datastax.oss.driver.api.mapper.annotations.ClusteringColumn;
import com.datastax.oss.driver.api.mapper.annotations.CqlName;
import com.datastax.oss.driver.api.mapper.annotations.Entity;
import com.datastax.oss.driver.api.mapper.annotations.PartitionKey;
import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Entity(defaultKeyspace = "booking_system")
@CqlName("bookings_by_user")
@Builder
public record BookingByUser(

        @PartitionKey
        UUID userId,

        @ClusteringColumn(1)
        UUID serviceId,

        @ClusteringColumn(2)
        LocalDate date,

        @ClusteringColumn(3)
        UUID bookingId,

        int start,

        int end
) {
}
