package com.github.matsik.cassandra.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.util.UUID;


@Getter
@Accessors(fluent = true)
@EqualsAndHashCode
public class BookingPartitionKey {

    private final LocalDate date;
    private final UUID serviceId;

    private BookingPartitionKey(LocalDate date, UUID serviceId) {
        this.date = date;
        this.serviceId = serviceId;
    }

    public static class Factory {
        public static BookingPartitionKey create(LocalDate date, UUID serviceId) {
            return new BookingPartitionKey(date, serviceId);
        }
    }
}
