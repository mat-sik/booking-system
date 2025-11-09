package com.github.matsik.query.grpc;

import com.google.type.Date;

import java.time.LocalDate;
import java.util.UUID;

public class GrpcMapper {

    public static Date grpcDate(LocalDate localDate) {
        return Date.newBuilder()
                .setDay(localDate.getDayOfMonth())
                .setMonth(localDate.getMonthValue())
                .setYear(localDate.getYear())
                .build();
    }

    public static LocalDate localDate(Date grpcDate) {
        return LocalDate.of(grpcDate.getYear(), grpcDate.getMonth(), grpcDate.getDay());
    }

    public static UUID uuid(String grpcUuid) {
        return UUID.fromString(grpcUuid);

    }
}
