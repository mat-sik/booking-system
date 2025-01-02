package com.github.matsik.mongo.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bson.types.ObjectId;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Getter
@Accessors(fluent = true)
@EqualsAndHashCode
public class ServiceBookingIdentifier {

    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final String date;
    private final ObjectId serviceId;

    private ServiceBookingIdentifier(LocalDate date, ObjectId serviceId) {
        this.date = date.format(FORMATTER);
        this.serviceId = serviceId;
    }

    private ServiceBookingIdentifier(String date, ObjectId serviceId) {
        this.date = date;
        this.serviceId = serviceId;
    }

    public static class Factory {
        public static ServiceBookingIdentifier create(String date, String serviceId) {
            LocalDate _ = LocalDate.parse(date, FORMATTER);
            ObjectId serviceIdMapped = new ObjectId(serviceId);
            return new ServiceBookingIdentifier(date, serviceIdMapped);
        }

        public static ServiceBookingIdentifier create(String date, ObjectId serviceId) {
            LocalDate _ = LocalDate.parse(date, FORMATTER);
            return new ServiceBookingIdentifier(date, serviceId);
        }

        public static ServiceBookingIdentifier create(LocalDate date, ObjectId serviceId) {
            return new ServiceBookingIdentifier(date, serviceId);
        }
    }
}
