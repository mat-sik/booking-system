package com.github.matsik.command.booking.repository;

import com.github.matsik.command.booking.command.CreateBookingCommand;
import com.github.matsik.command.booking.command.DeleteBookingCommand;
import com.github.matsik.command.booking.model.ServiceBooking;
import com.github.matsik.mongo.model.Booking;
import com.github.matsik.mongo.model.ServiceBookingIdentifier;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.result.UpdateResult;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.MongoDBContainer;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

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

    @BeforeEach
    void beforeEach() {
        MONGO_TEMPLATE.dropCollection(ServiceBooking.class);
    }

    private static Stream<Arguments> provideCreateBookingTestCases() {
        return Stream.of(
                Arguments.of(
                        "Successful booking creation, when required booking is present and has some bookings.",
                        (Runnable) () -> List.of(
                                new ServiceBooking(
                                        new ObjectId("000000000000000000000000"),
                                        LocalDate.of(2024, 12, 12).format(DateTimeFormatter.ISO_LOCAL_DATE),
                                        new ObjectId("100000000000000000000000"),
                                        List.of(
                                                new Booking(
                                                        new ObjectId("110000000000000000000000"),
                                                        new ObjectId("010000000000000000000000"),
                                                        0,
                                                        1000
                                                ),
                                                new Booking(
                                                        new ObjectId("110000000000000000000001"),
                                                        new ObjectId("010000000000000000000000"),
                                                        1100,
                                                        1440
                                                )
                                        )
                                )
                        ).forEach(MONGO_TEMPLATE::save),
                        new CreateBookingCommand(
                                ServiceBookingIdentifier.Factory.create(
                                        LocalDate.of(2024, 12, 12),
                                        new ObjectId("100000000000000000000000")
                                ),
                                new ObjectId("010000000000000000000001"),
                                1000,
                                1100
                        ),
                        true
                ),
                Arguments.of(
                        "Unsuccessful booking creation, when there is collision.",
                        (Runnable) () -> List.of(
                                new ServiceBooking(
                                        new ObjectId("000000000000000000000000"),
                                        LocalDate.of(2024, 12, 12).format(DateTimeFormatter.ISO_LOCAL_DATE),
                                        new ObjectId("100000000000000000000000"),
                                        List.of(
                                                new Booking(
                                                        new ObjectId("110000000000000000000000"),
                                                        new ObjectId("010000000000000000000000"),
                                                        0,
                                                        1100
                                                ),
                                                new Booking(
                                                        new ObjectId("110000000000000000000001"),
                                                        new ObjectId("010000000000000000000000"),
                                                        1100,
                                                        1440
                                                )
                                        )
                                )
                        ).forEach(MONGO_TEMPLATE::save),
                        new CreateBookingCommand(
                                ServiceBookingIdentifier.Factory.create(
                                        LocalDate.of(2024, 12, 12),
                                        new ObjectId("100000000000000000000000")
                                ),
                                new ObjectId("010000000000000000000001"),
                                1000,
                                1100
                        ),
                        false
                ),
                Arguments.of(
                        "Successful booking creation, when required document is not present(or not matched).",
                        (Runnable) () -> {
                        },
                        new CreateBookingCommand(
                                ServiceBookingIdentifier.Factory.create(
                                        LocalDate.of(2024, 12, 12),
                                        new ObjectId("100000000000000000000000")
                                ),
                                new ObjectId("010000000000000000000001"),
                                1000,
                                1100
                        ),
                        true
                ),
                Arguments.of(
                        "Successful booking creation, when required document is present, but empty.",
                        (Runnable) () -> List.of(
                                new ServiceBooking(
                                        new ObjectId("000000000000000000000000"),
                                        LocalDate.of(2024, 12, 12).format(DateTimeFormatter.ISO_LOCAL_DATE),
                                        new ObjectId("100000000000000000000000"),
                                        List.of()
                                )
                        ).forEach(MONGO_TEMPLATE::save), new CreateBookingCommand(
                                ServiceBookingIdentifier.Factory.create(
                                        LocalDate.of(2024, 12, 12),
                                        new ObjectId("100000000000000000000000")
                                ),
                                new ObjectId("010000000000000000000001"),
                                1000,
                                1100
                        ),
                        true
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideCreateBookingTestCases")
    void createBooking(
            String name,
            Runnable createPreExistingBookings,
            CreateBookingCommand command,
            boolean isSuccessful
    ) {
        // given
        createPreExistingBookings.run();

        // when
        UpdateResult result = REPOSITORY.createBooking(command);

        // then
        long matched = result.getMatchedCount();
        long modified = result.getModifiedCount();
        if (isSuccessful) {
            assertThat(matched).isOne();
            assertThat(modified).isOne();
        } else {
            assertThat(matched).isZero();
            assertThat(modified).isZero();
        }
    }

    private static Stream<Arguments> provideDeleteBookingTestCases() {
        return Stream.of(
                Arguments.of(
                        "Successful deletion, when document and booking exist.",
                        (Runnable) () -> List.of(
                                new ServiceBooking(
                                        new ObjectId("000000000000000000000000"),
                                        LocalDate.of(2024, 12, 12).format(DateTimeFormatter.ISO_LOCAL_DATE),
                                        new ObjectId("100000000000000000000000"),
                                        List.of(
                                                new Booking(
                                                        new ObjectId("110000000000000000000000"),
                                                        new ObjectId("010000000000000000000000"),
                                                        0,
                                                        1100
                                                )
                                        )
                                )
                        ).forEach(MONGO_TEMPLATE::save),
                        new DeleteBookingCommand(
                                ServiceBookingIdentifier.Factory.create(
                                        LocalDate.of(2024, 12, 12),
                                        new ObjectId("100000000000000000000000")
                                ),
                                new ObjectId("110000000000000000000000")
                        ),
                        true,
                        false
                ),
                Arguments.of(
                        "Unsuccessful deletion, when document exist but booking doesn't.",
                        (Runnable) () -> List.of(
                                new ServiceBooking(
                                        new ObjectId("000000000000000000000000"),
                                        LocalDate.of(2024, 12, 12).format(DateTimeFormatter.ISO_LOCAL_DATE),
                                        new ObjectId("100000000000000000000000"),
                                        List.of()
                                )
                        ).forEach(MONGO_TEMPLATE::save),
                        new DeleteBookingCommand(
                                ServiceBookingIdentifier.Factory.create(
                                        LocalDate.of(2024, 12, 12),
                                        new ObjectId("100000000000000000000000")
                                ),
                                new ObjectId("110000000000000000000000")
                        ),
                        false,
                        true
                ),
                Arguments.of(
                        "Unsuccessful deletion, when document doesn't exist.",
                        (Runnable) () -> {
                        },
                        new DeleteBookingCommand(
                                ServiceBookingIdentifier.Factory.create(
                                        LocalDate.of(2024, 12, 12),
                                        new ObjectId("100000000000000000000000")
                                ),
                                new ObjectId("110000000000000000000000")
                        ),
                        false,
                        false
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideDeleteBookingTestCases")
    void deleteBooking(
            String name,
            Runnable createPreExistingBookings,
            DeleteBookingCommand command,
            boolean isSuccessful,
            boolean matchedButNoBooking
    ) {
        // given
        createPreExistingBookings.run();

        // when
        UpdateResult result = REPOSITORY.deleteBooking(command);

        // then
        long matched = result.getMatchedCount();
        long modified = result.getModifiedCount();

        if (matchedButNoBooking) {
            assertThat(matched).isOne();
            assertThat(modified).isZero();
            return;
        }

        if (isSuccessful) {
            assertThat(matched).isOne();
            assertThat(modified).isOne();
        } else {
            assertThat(matched).isZero();
            assertThat(modified).isZero();
        }
    }

}