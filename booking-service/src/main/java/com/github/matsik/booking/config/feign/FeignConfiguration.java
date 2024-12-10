package com.github.matsik.booking.config.feign;

import org.springframework.cloud.openfeign.FeignFormatterRegistrar;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;

import java.time.format.DateTimeFormatter;

@Configuration
public class FeignConfiguration {

    @Bean
    public FeignFormatterRegistrar feignFormatterRegistrar() {
        return formatterRegistry -> {
            DateTimeFormatterRegistrar registrar =
                    new DateTimeFormatterRegistrar();
            registrar.setDateFormatter(DateTimeFormatter.ISO_LOCAL_DATE);
            registrar.registerFormatters(formatterRegistry);
        };
    }

}
