package com.github.matsik.booking;

import com.github.matsik.kafka.task.CommandValue;
import com.github.matsik.kafka.task.CreateBookingCommandValue;
import lombok.extern.java.Log;
import org.bson.types.ObjectId;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDate;

@SpringBootApplication
@ConfigurationPropertiesScan
@Log
public class BookingServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(BookingServiceApplication.class, args);
	}

	@Bean
	public CommandLineRunner commandLineRunner(KafkaTemplate<LocalDate, CommandValue> kafkaTemplate) {
		return _ -> {
			LocalDate date = LocalDate.of(2024, 12, 6);
			ObjectId serviceId = new ObjectId();
			ObjectId userId = new ObjectId();
			for (int start = 0; start < 1000; start += 100) {
				int end = start + 100;
				CommandValue commandValue = new CreateBookingCommandValue(serviceId, userId, start, end);

				kafkaTemplate.send("bookings", date, commandValue);
				log.info("send");
			}
		};
	}

}
