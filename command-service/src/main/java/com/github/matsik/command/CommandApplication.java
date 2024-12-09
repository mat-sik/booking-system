package com.github.matsik.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.matsik.command.client.kafka.KafkaClientProperties;
import com.github.matsik.kafka.mapping.LocalDateSerializer;
import com.github.matsik.kafka.task.CommandValue;
import com.github.matsik.kafka.task.CreateBookingCommandValue;
import lombok.extern.java.Log;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.bson.types.ObjectId;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.time.LocalDate;
import java.util.Map;

@SpringBootApplication
@ConfigurationPropertiesScan
@Log
public class CommandApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommandApplication.class, args);
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

    @Bean
    public ProducerFactory<LocalDate, CommandValue> producerFactory(
            KafkaClientProperties kafkaClientProperties,
            ObjectMapper objectMapper
    ) {
        Map<String, Object> props = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaClientProperties.hosts()
        );

        JsonSerializer<CommandValue> jsonSerializer = new JsonSerializer<>(objectMapper);

        return new DefaultKafkaProducerFactory<>(
                props,
                new LocalDateSerializer(),
                jsonSerializer
        );
    }

    @Bean
    public KafkaTemplate<LocalDate, CommandValue> kafkaTemplate(ProducerFactory<LocalDate, CommandValue> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
