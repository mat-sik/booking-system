package com.github.matsik.command.client.kafka;

import com.github.matsik.kafka.mapping.LocalDateDeserializer;
import com.github.matsik.kafka.task.CommandValue;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.time.LocalDate;
import java.util.Map;

@Configuration
public class KafkaClientConfiguration {

    @Bean
    public KafkaAdmin admin(KafkaClientProperties kafkaClientProperties) {
        Map<String, Object> configs = Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaClientProperties.hosts()
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
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<LocalDate, CommandValue>> kafkaListenerContainerFactory(
            ConsumerFactory<LocalDate, CommandValue> localDateCommandValueConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<LocalDate, CommandValue> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(localDateCommandValueConsumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        return factory;
    }

    @Bean
    public ConsumerFactory<LocalDate, CommandValue> localDateCommandValueConsumerFactory(
            KafkaClientProperties kafkaClientProperties,
            JsonDeserializer<CommandValue> commandValueJsonDeserializer
    ) {
        Map<String, Object> consumerConfig = consumerConfig(kafkaClientProperties);
        return new DefaultKafkaConsumerFactory<>(consumerConfig, new LocalDateDeserializer(), commandValueJsonDeserializer);
    }

    private Map<String, Object> consumerConfig(KafkaClientProperties kafkaClientProperties) {
        return Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaClientProperties.hosts(),
                ConsumerConfig.GROUP_ID_CONFIG, kafkaClientProperties.groupId(),
                ConsumerConfig.CLIENT_ID_CONFIG, kafkaClientProperties.clientId(),
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                JsonDeserializer.TRUSTED_PACKAGES, "com.github.matsik.kafka.task"
        );
    }

}