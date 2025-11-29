package com.github.matsik.kafka.mapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.matsik.kafka.task.CommandValue;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

@RequiredArgsConstructor
public class CommandValueDeserializer implements Deserializer<CommandValue> {

    private final ObjectMapper objectMapper;

    @Override
    public CommandValue deserialize(String s, byte[] bytes) {
        if (bytes == null) {
            return null;
        }

        try {
            return objectMapper.readValue(bytes, CommandValue.class);
        } catch (Exception e) {
            throw new SerializationException("Error deserializing JSON message to CommandValue", e);
        }
    }

}
