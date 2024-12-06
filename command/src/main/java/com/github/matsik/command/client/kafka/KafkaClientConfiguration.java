package com.github.matsik.command.client.kafka;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

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
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, String>> kafkaListenerContainerFactory(KafkaClientProperties kafkaClientProperties) {
        Map<String, Object> consumerConfig = consumerConfig(kafkaClientProperties);

        ConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerConfig);

        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        return factory;
    }

    public Map<String, Object> consumerConfig(KafkaClientProperties kafkaClientProperties) {
        return Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,  kafkaClientProperties.hosts(),
                ConsumerConfig.GROUP_ID_CONFIG, kafkaClientProperties.groupId(),
                ConsumerConfig.CLIENT_ID_CONFIG, kafkaClientProperties.clientId(),
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class
        );
    }

}
