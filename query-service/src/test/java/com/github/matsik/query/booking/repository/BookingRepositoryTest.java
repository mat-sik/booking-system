package com.github.matsik.query.booking.repository;

import com.github.matsik.mongo.model.Booking;
import com.github.matsik.mongo.model.ServiceBookingIdentifier;
import com.github.matsik.query.booking.model.ServiceBooking;
import com.github.matsik.query.booking.model.UserBooking;
import com.github.matsik.query.booking.query.GetBookingQuery;
import com.github.matsik.query.booking.query.GetBookingTimeRangesQuery;
import com.github.matsik.query.booking.query.GetBookingsQuery;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.MongoDBContainer;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class BookingRepositoryTest {

    private static final MongoDBContainer MONGO_DB_CONTAINER;
    private static final MongoClient MONGO_CLIENT;
    private static final MongoTemplate MONGO_TEMPLATE;
    private static final BookingRepository REPOSITORY;

    private static final List<ServiceBooking> SERVICE_BOOKINGS;

    static {
        MONGO_DB_CONTAINER = new MongoDBContainer("mongo:8.0.0");
        MONGO_DB_CONTAINER.start();

        MONGO_CLIENT = MongoClients.create(MONGO_DB_CONTAINER.getReplicaSetUrl());
        MONGO_TEMPLATE = new MongoTemplate(MONGO_CLIENT, "test");

        REPOSITORY = new BookingRepository(MONGO_TEMPLATE);

        SERVICE_BOOKINGS = List.of(
                new ServiceBooking(
                        new ObjectId("000000000000000000000000"),
                        LocalDate.of(2024, 12, 12).format(DateTimeFormatter.ISO_LOCAL_DATE),
                        new ObjectId("100000000000000000000000"),
                        List.of(
                                new Booking(
                                        new ObjectId("110000000000000000000000"),
                                        new ObjectId("010000000000000000000000"),
                                        0,
                                        300
                                ),
                                new Booking(
                                        new ObjectId("110000000000000000000001"),
                                        new ObjectId("010000000000000000000000"),
                                        300,
                                        600
                                ),
                                new Booking(
                                        new ObjectId("110000000000000000000002"),
                                        new ObjectId("010000000000000000000001"),
                                        600,
                                        900
                                )
                        )
                ),
                new ServiceBooking(
                        new ObjectId("000000000000000000000001"),
                        LocalDate.of(2024, 12, 13).format(DateTimeFormatter.ISO_LOCAL_DATE),
                        new ObjectId("100000000000000000000000"),
                        List.of(
                                new Booking(
                                        new ObjectId("110000000000000000000004"),
                                        new ObjectId("010000000000000000000000"),
                                        330,
                                        630
                                ),
                                new Booking(
                                        new ObjectId("110000000000000000000005"),
                                        new ObjectId("010000000000000000000001"),
                                        630,
                                        1200
                                )
                        )
                ),
                new ServiceBooking(
                        new ObjectId("000000000000000000000002"),
                        LocalDate.of(2024, 12, 13).format(DateTimeFormatter.ISO_LOCAL_DATE),
                        new ObjectId("100000000000000000000001"),
                        List.of(
                                new Booking(
                                        new ObjectId("110000000000000000000006"),
                                        new ObjectId("010000000000000000000000"),
                                        330,
                                        630
                                )

                        )
                )
        );

        SERVICE_BOOKINGS.forEach(MONGO_TEMPLATE::save);
    }

    @AfterAll
    static void afterAll() {
        MONGO_CLIENT.close();
        MONGO_DB_CONTAINER.close();
    }

    private static Stream<Object[]> provideGetUserBookingTestCases() {
        return Stream.of(
                getTestCase(0, 0),
                getTestCase(0, 1),
                getTestCase(0, 2),
                getTestCase(1, 0),
                getTestCase(1, 1),
                getTestCase(2, 0),
                getTestCase(-1, -1)
        );
    }

    private static Object[] getTestCase(int serviceBookingIdx, int bookingIdx) {
        GetBookingQuery query = getBookingQueryForNonExisting();
        UserBooking expected = null;
        boolean isEmpty = true;
        if (serviceBookingIdx != -1 && bookingIdx != -1) {
            query = getGetBookingQuery(serviceBookingIdx, bookingIdx);
            expected = getExpected(serviceBookingIdx, bookingIdx);
            isEmpty = false;
        }
        return new Object[]{query, expected, isEmpty};
    }

    private static UserBooking getExpected(int serviceBookingIdx, int bookingIdx) {
        ServiceBooking serviceBooking = SERVICE_BOOKINGS.get(serviceBookingIdx);
        Booking booking = serviceBooking.bookings().get(bookingIdx);
        return new UserBooking(booking.userId(), booking.start(), booking.end());
    }

    private static GetBookingQuery getGetBookingQuery(int serviceBookingIdx, int bookingIdx) {
        ServiceBooking serviceBooking = SERVICE_BOOKINGS.get(serviceBookingIdx);
        Booking booking = serviceBooking.bookings().get(bookingIdx);

        LocalDate localDate = LocalDate.parse(serviceBooking.date(), DateTimeFormatter.ISO_LOCAL_DATE);
        ObjectId serviceId = serviceBooking.serviceId();

        ServiceBookingIdentifier identifier = ServiceBookingIdentifier.Factory.create(localDate, serviceId);

        ObjectId bookingId = booking.id();
        return new GetBookingQuery(identifier, bookingId);
    }

    private static GetBookingQuery getBookingQueryForNonExisting() {
        LocalDate localDate = LocalDate.of(2000, 1, 1);
        ObjectId serviceId = new ObjectId("aaaaaaaaaaaaaaaaaaaaaaaa");
        ObjectId bookingId = new ObjectId("bbbbbbbbbbbbbbbbbbbbbbbb");
        ServiceBookingIdentifier identifier = ServiceBookingIdentifier.Factory.create(localDate, serviceId);
        return new GetBookingQuery(identifier, bookingId);
    }

    @ParameterizedTest
    @MethodSource("provideGetUserBookingTestCases")
    void testGetUserBooking(GetBookingQuery query, UserBooking expected, boolean isEmpty) {
        // when
        Optional<UserBooking> result = REPOSITORY.getUserBooking(query);

        // then
        if (!isEmpty) {
            assertThat(result).isNotEmpty();
            assertThat(result.get()).isEqualTo(expected);
        } else {
            assertThat(result).isEmpty();
        }
    }

    @Test
    void getBookings() {
        List<LocalDate> dates = List.of(
                LocalDate.of(2024, 12, 3),
                LocalDate.of(2024, 12, 4)
        );
        List<ObjectId> serviceIds = List.of(new ObjectId("aaaaaaaaaaaaaaaaaaaaaaaa"));
        List<ObjectId> userIds = List.of(new ObjectId("bbbbbbbbbbbbbbbbbbbbbbbb"));
        GetBookingsQuery request = new GetBookingsQuery(dates, serviceIds, userIds);
        var out = REPOSITORY.getBookings(request);
        System.out.println(out);
    }

    @Test
    void getBookingTimeRanges() {
        LocalDate localDate = LocalDate.of(2024, 12, 3);
        ObjectId serviceId = new ObjectId("aaaaaaaaaaaaaaaaaaaaaaaa");
        ServiceBookingIdentifier identifier = ServiceBookingIdentifier.Factory.create(localDate, serviceId);
        GetBookingTimeRangesQuery request = new GetBookingTimeRangesQuery(identifier);
        var out = REPOSITORY.getBookingTimeRanges(request);
        System.out.println(out);
    }
}