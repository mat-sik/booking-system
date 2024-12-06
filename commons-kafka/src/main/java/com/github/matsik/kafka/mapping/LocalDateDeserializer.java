package com.github.matsik.kafka.mapping;

import org.apache.kafka.common.serialization.Deserializer;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

public class LocalDateDeserializer implements Deserializer<LocalDate> {
    @Override
    public LocalDate deserialize(String s, byte[] bytes) {
        return LocalDate.parse(new String(bytes, StandardCharsets.UTF_8), LocalDateSerializer.FORMATTER);
    }
}
