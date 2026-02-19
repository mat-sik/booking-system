package com.github.matsik.booking.config.kafka;

import com.github.matsik.dto.BookingPartitionKey;
import com.github.matsik.kafka.task.CommandValue;
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

import java.util.Map;

@Configuration
public class KafkaClientConfiguration {

    public static final String BOOKINGS_TOPIC_NAME = "bookings";

    @Bean
    public KafkaAdmin admin(KafkaClientProperties kafkaClientProperties) {
        Map<String, Object> configs = Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaClientProperties.bootstrapServers()
        );
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic bookingsTopic() {
        return TopicBuilder.name(BOOKINGS_TOPIC_NAME)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public ProducerFactory<BookingPartitionKey, CommandValue> producerFactory(
            KafkaClientProperties kafkaClientProperties
    ) {
        Map<String, Object> props = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaClientProperties.bootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "com.github.matsik.kafka.mapping.BookingPartitionKeySerializer",
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "com.github.matsik.kafka.mapping.CommandValueSerializer"
        );

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<BookingPartitionKey, CommandValue> kafkaTemplate(ProducerFactory<BookingPartitionKey, CommandValue> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

}
