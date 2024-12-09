package com.github.matsik.booking.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.matsik.jackson.ObjectMapperFactory;
import com.github.matsik.kafka.task.CommandValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonDeserializer;

@Configuration
public class JacksonConfiguration {

    @Bean
    public ObjectMapper objectMapper() {
        return ObjectMapperFactory.create();
    }

    @Bean
    public JsonDeserializer<CommandValue> commandValueJsonDeserializer(ObjectMapper objectMapper) {
        return new JsonDeserializer<>(objectMapper);
    }

}
