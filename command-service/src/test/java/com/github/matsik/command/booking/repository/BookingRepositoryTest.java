package com.github.matsik.command.booking.repository;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.MongoDBContainer;

class BookingRepositoryTest {

    private static final MongoDBContainer MONGO_DB_CONTAINER;
    private static final MongoClient MONGO_CLIENT;
    private static final MongoTemplate MONGO_TEMPLATE;
    private static final BookingRepository REPOSITORY;

    static {
        MONGO_DB_CONTAINER = new MongoDBContainer("mongo:8.0.0");
        MONGO_DB_CONTAINER.start();

        MONGO_CLIENT = MongoClients.create(MONGO_DB_CONTAINER.getReplicaSetUrl());
        MONGO_TEMPLATE = new MongoTemplate(MONGO_CLIENT, "test");

        REPOSITORY = new BookingRepository(MONGO_TEMPLATE);
    }

    @AfterAll
    static void afterAll() {
        MONGO_CLIENT.close();
        MONGO_DB_CONTAINER.close();
    }

    @Test
    void createBooking() {

    }

    @Test
    void deleteBooking() {
    }

}