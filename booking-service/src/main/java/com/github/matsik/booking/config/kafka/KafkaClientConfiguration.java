package com.github.matsik.booking.config.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.matsik.kafka.mapping.ServiceBookingIdentifierSerializer;
import com.github.matsik.kafka.task.CommandValue;
import com.github.matsik.mongo.model.ServiceBookingIdentifier;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Map;

@Configuration
public class KafkaClientConfiguration {

    @Bean
    public KafkaAdmin admin(KafkaClientProperties kafkaClientProperties) {
        Map<String, Object> configs = Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaClientProperties.bootstrapServers()
        );
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic bookingsTopic() {
        return TopicBuilder.name("bookings")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public ProducerFactory<ServiceBookingIdentifier, CommandValue> producerFactory(
            KafkaClientProperties kafkaClientProperties,
            ObjectMapper objectMapper
    ) {
        Map<String, Object> props = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaClientProperties.bootstrapServers()
        );

        JsonSerializer<CommandValue> jsonSerializer = new JsonSerializer<>(objectMapper);

        return new DefaultKafkaProducerFactory<>(
                props,
                new ServiceBookingIdentifierSerializer(),
                jsonSerializer
        );
    }

    @Bean
    public KafkaTemplate<ServiceBookingIdentifier, CommandValue> kafkaTemplate(ProducerFactory<ServiceBookingIdentifier, CommandValue> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

}
