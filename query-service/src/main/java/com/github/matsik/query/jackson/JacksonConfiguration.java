package com.github.matsik.query.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.matsik.jackson.ObjectMapperFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfiguration {

    @Bean
    public ObjectMapper objectMapper() {
        return ObjectMapperFactory.create();
    }

}
