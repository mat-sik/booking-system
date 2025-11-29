package com.github.matsik.kafka.mapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.matsik.kafka.task.CommandValue;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

@RequiredArgsConstructor
public class CommandValueSerializer implements Serializer<CommandValue> {

    private final ObjectMapper objectMapper;

    @Override
    public byte[] serialize(String s, CommandValue commandValue) {
        if (commandValue == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsBytes(commandValue);
        } catch (Exception e) {
            throw new SerializationException("Error serializing CommandValue to JSON", e);
        }
    }

}
