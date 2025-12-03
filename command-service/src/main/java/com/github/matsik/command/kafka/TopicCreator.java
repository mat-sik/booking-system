package com.github.matsik.command.kafka;

import com.github.matsik.command.config.kafka.KafkaProperties;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@Component
public class TopicCreator {

    private final Admin admin;
    private final KafkaProperties.TopicDefaults kafkaProperties;

    public TopicCreator(Admin admin, KafkaProperties kafkaProperties) {
        this.admin = admin;
        this.kafkaProperties = kafkaProperties.topics();
    }

    void ensureBookingTopicExists() {
        String bookingTopicName = kafkaProperties.bookingTopicName();
        int partitions = kafkaProperties.partitions();
        short replicationFactor = kafkaProperties.replicationFactor();

        if (!topicExists(bookingTopicName)) {
            createTopics(bookingTopicName, partitions, replicationFactor);
        }
    }

    private boolean topicExists(String topicName) {
        try {
            ListTopicsResult listTopics = admin.listTopics();
            Set<String> remoteNames = listTopics.names().get();

            return remoteNames.contains(topicName);
        } catch (ExecutionException | InterruptedException ex) {
            throw new RuntimeException("Could not list topics.", ex);
        }
    }

    private void createTopics(String name, int partitions, short replicationFactor) {
        List<NewTopic> topics = List.of(new NewTopic(name, partitions, replicationFactor));
        CreateTopicsResult future = admin.createTopics(topics);

        try {
            future.all().get();
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException("Unable to create topics.", ex);
        }
    }

}
