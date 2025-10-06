package com.github.matsik.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.util.UUID;


@Getter
@Accessors(fluent = true)
@EqualsAndHashCode
public class BookingPartitionKey {

    private final UUID serviceId;
    private final LocalDate date;

    private BookingPartitionKey(UUID serviceId, LocalDate date) {
        this.serviceId = serviceId;
        this.date = date;
    }

    public static class Factory {
        public static BookingPartitionKey create(UUID serviceId, LocalDate date) {
            return new BookingPartitionKey(serviceId, date);
        }
    }
}
