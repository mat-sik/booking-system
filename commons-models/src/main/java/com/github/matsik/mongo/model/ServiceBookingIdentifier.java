package com.github.matsik.mongo.model;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.bson.types.ObjectId;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Getter
@Accessors(fluent = true)
public class ServiceBookingIdentifier {

    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final String date;
    private final ObjectId serviceId;

    private ServiceBookingIdentifier(LocalDate date, ObjectId serviceId) {
        this.date = date.format(FORMATTER);
        this.serviceId = serviceId;
    }

    public static class Factory {
        public static ServiceBookingIdentifier create(String date, String serviceId) {
            LocalDate localDate = LocalDate.parse(date, FORMATTER);
            ObjectId serviceIdMapped = new ObjectId(serviceId);
            return new ServiceBookingIdentifier(localDate, serviceIdMapped);
        }

        public static ServiceBookingIdentifier create(LocalDate date, ObjectId serviceId) {
            return new ServiceBookingIdentifier(date, serviceId);
        }
    }
}
