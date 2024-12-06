package com.github.matsik.kafka.mapping;

import com.github.matsik.mongo.model.ServiceBookingIdentifier;
import org.apache.kafka.common.serialization.Serializer;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

public class LocalDateSerializer implements Serializer<LocalDate> {
    @Override
    public byte[] serialize(String s, LocalDate localDate) {
        return localDate.format(ServiceBookingIdentifier.FORMATTER).getBytes(StandardCharsets.UTF_8);
    }
}
