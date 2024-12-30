package com.github.matsik.kafka.mapping;

import org.apache.kafka.common.serialization.Deserializer;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

public class LocalDateDeserializer implements Deserializer<LocalDate> {
    @Override
    public LocalDate deserialize(String topic, byte[] localDateBytes) {
        return LocalDate.parse(new String(localDateBytes, StandardCharsets.UTF_8), LocalDateSerializer.FORMATTER);
    }
}
