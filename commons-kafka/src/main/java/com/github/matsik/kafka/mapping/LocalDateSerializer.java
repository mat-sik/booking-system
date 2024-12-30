package com.github.matsik.kafka.mapping;

import org.apache.kafka.common.serialization.Serializer;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class LocalDateSerializer implements Serializer<LocalDate> {
    final static DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    @Override
    public byte[] serialize(String topic, LocalDate localDate) {
        return localDate.format(FORMATTER).getBytes(StandardCharsets.UTF_8);
    }
}
