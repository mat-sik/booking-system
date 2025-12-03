package com.github.matsik.command.config.kafka;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class KafkaClientConfiguration {

    @Bean
    public Admin kafkaAdmin(Properties kafkaAdminProperties) {
        return Admin.create(kafkaAdminProperties);
    }

    @Bean
    public Properties kafkaAdminProperties(KafkaProperties kafkaProperties) {
        var adminProperties = new Properties();

        adminProperties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.client().bootstrapServers());

        return adminProperties;
    }

    @Bean
    public Properties kafkaConsumerProperties(KafkaProperties kafkaProperties) {
        var consumerProperties = new Properties();

        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.client().bootstrapServers());
        consumerProperties.put(ConsumerConfig.CLIENT_ID_CONFIG, kafkaProperties.client().clientId());

        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaProperties.consumer().groupId());

        consumerProperties.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, kafkaProperties.consumer().maxPollIntervalMs());
        consumerProperties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, kafkaProperties.consumer().maxPollRecords());

        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "com.github.matsik.kafka.mapping.BookingPartitionKeyDeserializer");
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "com.github.matsik.kafka.mapping.CommandValueDeserializer");

        consumerProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return consumerProperties;
    }

}
