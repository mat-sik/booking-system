package com.github.matsik.command.booking.command;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.bson.types.ObjectId;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Getter
@Accessors(fluent = true)
public class ServiceBookingIdentifier {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final String date;
    private final ObjectId serviceId;

    public ServiceBookingIdentifier(LocalDate date, ObjectId serviceId) {
        this.date = date.format(FORMATTER);
        this.serviceId = serviceId;
    }
}