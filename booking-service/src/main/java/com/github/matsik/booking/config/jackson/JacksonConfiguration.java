package com.github.matsik.booking.config.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.matsik.jackson.ObjectMapperFactory;
import com.github.matsik.kafka.task.CommandValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonDeserializer;

@Configuration
public class JacksonConfiguration {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = ObjectMapperFactory.create();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }

    @Bean
    public JsonDeserializer<CommandValue> commandValueJsonDeserializer(ObjectMapper objectMapper) {
        return new JsonDeserializer<>(objectMapper);
    }

}
