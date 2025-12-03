package com.github.matsik.kafka.mapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.matsik.kafka.task.CommandValue;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

public class CommandValueDeserializer implements Deserializer<CommandValue> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public CommandValue deserialize(String s, byte[] bytes) {
        if (bytes == null) {
            return null;
        }

        try {
            return OBJECT_MAPPER.readValue(bytes, CommandValue.class);
        } catch (Exception e) {
            throw new SerializationException("Error deserializing JSON message to CommandValue", e);
        }
    }

}
