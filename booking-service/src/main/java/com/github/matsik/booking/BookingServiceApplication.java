package com.github.matsik.booking;

import com.github.matsik.booking.query.QueryClient;
import com.github.matsik.query.response.TimeRangeResponse;
import lombok.extern.java.Log;
import org.bson.types.ObjectId;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

@SpringBootApplication
@ConfigurationPropertiesScan
@Log
@EnableFeignClients(basePackages = "com.github.matsik.booking.query")
public class BookingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookingServiceApplication.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(QueryClient client) {
        return _ -> {
            LocalDate localDate = LocalDate.of(2024, 12, 8);
            ObjectId serviceId = new ObjectId();
            int serviceDuration = 30;

            ResponseEntity<List<TimeRangeResponse>> response = client.getAvailableTimeRanges(localDate, serviceId, serviceDuration);
            System.out.println(response);
        };
    }

}
