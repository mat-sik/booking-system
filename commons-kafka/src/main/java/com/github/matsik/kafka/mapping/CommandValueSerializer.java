package com.github.matsik.kafka.mapping;

import com.github.matsik.kafka.task.CommandValue;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;
import tools.jackson.databind.ObjectMapper;

public class CommandValueSerializer implements Serializer<CommandValue> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public byte[] serialize(String s, CommandValue commandValue) {
        if (commandValue == null) {
            return null;
        }

        try {
            return OBJECT_MAPPER.writeValueAsBytes(commandValue);
        } catch (Exception e) {
            throw new SerializationException("Error serializing CommandValue to JSON", e);
        }
    }

}
