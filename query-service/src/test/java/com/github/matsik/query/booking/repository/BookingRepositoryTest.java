package com.github.matsik.query.booking.repository;

import com.github.matsik.mongo.model.Booking;
import com.github.matsik.mongo.model.ServiceBookingIdentifier;
import com.github.matsik.query.booking.model.ServiceBooking;
import com.github.matsik.query.booking.model.UserBooking;
import com.github.matsik.query.booking.query.GetBookingQuery;
import com.github.matsik.query.booking.query.GetBookingTimeRangesQuery;
import com.github.matsik.query.booking.query.GetBookingsQuery;
import com.github.matsik.query.booking.service.TimeRange;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
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

        REPOSITORY = new BookingRepositoryMongo(MONGO_TEMPLATE);

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

    private static Stream<Arguments> provideGetUserBookingTestCases() {
        return Stream.of(
                getTestCase("serviceBooking-0, booking-0", 0, 0),
                getTestCase("serviceBooking-0, booking-1", 0, 1),
                getTestCase("serviceBooking-0, booking-2", 0, 2),
                getTestCase("serviceBooking-1, booking-0", 1, 0),
                getTestCase("serviceBooking-1, booking-1", 1, 1),
                getTestCase("serviceBooking-2, booking-0", 2, 0),
                getTestCase("None matching.", -1, -1)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideGetUserBookingTestCases")
    void testGetUserBooking(String name, GetBookingQuery query, UserBooking expected, boolean isEmpty) {
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

    private static Arguments getTestCase(String testName, int serviceBookingIdx, int bookingIdx) {
        GetBookingQuery query = getBookingQueryForNonExisting();
        UserBooking expected = null;
        boolean isEmpty = true;

        if (serviceBookingIdx != -1 && bookingIdx != -1) {
            query = getGetBookingQuery(serviceBookingIdx, bookingIdx);
            expected = getExpected(serviceBookingIdx, bookingIdx);
            isEmpty = false;
        }

        return Arguments.of(testName, query, expected, isEmpty);
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

    private static Stream<Arguments> provideGetBookingsTestCases() {
        return Stream.of(
                Arguments.of("No filtering, return all.",
                        new GetBookingsQuery(
                                List.of(), List.of(), List.of()),
                        SERVICE_BOOKINGS
                ),
                Arguments.of("Filter for one specific by date and service id.",
                        new GetBookingsQuery(
                                List.of(
                                        LocalDate.parse(SERVICE_BOOKINGS.get(1).date(), DateTimeFormatter.ISO_LOCAL_DATE)
                                ),
                                List.of(
                                        SERVICE_BOOKINGS.get(1).serviceId()
                                ),
                                List.of()
                        ),
                        List.of(
                                SERVICE_BOOKINGS.get(1)
                        )
                ),
                Arguments.of("Filter for one specific by date and service id, filter by one user.",
                        new GetBookingsQuery(
                                List.of(
                                        LocalDate.parse(SERVICE_BOOKINGS.get(1).date(), DateTimeFormatter.ISO_LOCAL_DATE)
                                ),
                                List.of(
                                        SERVICE_BOOKINGS.get(1).serviceId()
                                ),
                                List.of(
                                        SERVICE_BOOKINGS.get(1).bookings().get(0).userId()
                                )
                        ),
                        List.of(
                                new ServiceBooking(
                                        SERVICE_BOOKINGS.get(1).id(),
                                        SERVICE_BOOKINGS.get(1).date(),
                                        SERVICE_BOOKINGS.get(1).serviceId(),
                                        List.of(
                                                SERVICE_BOOKINGS.get(1).bookings().get(0)
                                        )
                                )
                        )
                ),
                Arguments.of("Filter by date.",
                        new GetBookingsQuery(
                                List.of(
                                        LocalDate.parse(SERVICE_BOOKINGS.get(1).date(), DateTimeFormatter.ISO_LOCAL_DATE)
                                ),
                                List.of(),
                                List.of()
                        ),
                        List.of(
                                SERVICE_BOOKINGS.get(1),
                                SERVICE_BOOKINGS.get(2)
                        )
                ),
                Arguments.of("Filter by service id.",
                        new GetBookingsQuery(
                                List.of(),
                                List.of(
                                        SERVICE_BOOKINGS.get(0).serviceId()
                                ),
                                List.of()
                        ),
                        List.of(
                                SERVICE_BOOKINGS.get(0),
                                SERVICE_BOOKINGS.get(1)
                        )
                ),
                Arguments.of("Filter by user.",
                        new GetBookingsQuery(
                                List.of(),
                                List.of(),
                                List.of(
                                        SERVICE_BOOKINGS.get(0).bookings().get(0).userId()
                                )
                        ),
                        List.of(
                                new ServiceBooking(
                                        SERVICE_BOOKINGS.get(0).id(),
                                        SERVICE_BOOKINGS.get(0).date(),
                                        SERVICE_BOOKINGS.get(0).serviceId(),
                                        List.of(
                                                SERVICE_BOOKINGS.get(0).bookings().get(0),
                                                SERVICE_BOOKINGS.get(0).bookings().get(1)
                                        )
                                ),
                                new ServiceBooking(
                                        SERVICE_BOOKINGS.get(1).id(),
                                        SERVICE_BOOKINGS.get(1).date(),
                                        SERVICE_BOOKINGS.get(1).serviceId(),
                                        List.of(
                                                SERVICE_BOOKINGS.get(1).bookings().get(0)
                                        )
                                ),
                                new ServiceBooking(
                                        SERVICE_BOOKINGS.get(2).id(),
                                        SERVICE_BOOKINGS.get(2).date(),
                                        SERVICE_BOOKINGS.get(2).serviceId(),
                                        List.of(
                                                SERVICE_BOOKINGS.get(2).bookings().get(0)
                                        )
                                )
                        )
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideGetBookingsTestCases")
    void getBookings(String name, GetBookingsQuery query, List<ServiceBooking> expected) {
        // when
        List<ServiceBooking> result = REPOSITORY.getBookings(query);

        // then
        assertThat(result).isEqualTo(expected);
    }

    private static Stream<Arguments> provideGetBookingTimeRangesTestCases() {
        return Stream.of(
                Arguments.of("First service booking.",
                        getGetBookingTimeRangesQuery(
                                SERVICE_BOOKINGS.get(0).date(),
                                SERVICE_BOOKINGS.get(0).serviceId()
                        ),
                        List.of(
                                bookingTimeRangeFrom(SERVICE_BOOKINGS.get(0).bookings().get(0)),
                                bookingTimeRangeFrom(SERVICE_BOOKINGS.get(0).bookings().get(1)),
                                bookingTimeRangeFrom(SERVICE_BOOKINGS.get(0).bookings().get(2))
                        )
                ),
                Arguments.of("Second service booking.",
                        getGetBookingTimeRangesQuery(
                                SERVICE_BOOKINGS.get(1).date(),
                                SERVICE_BOOKINGS.get(1).serviceId()
                        ),
                        List.of(
                                bookingTimeRangeFrom(SERVICE_BOOKINGS.get(1).bookings().get(0)),
                                bookingTimeRangeFrom(SERVICE_BOOKINGS.get(1).bookings().get(1))
                        )
                ),
                Arguments.of("Third service booking.",
                        getGetBookingTimeRangesQuery(
                                SERVICE_BOOKINGS.get(2).date(),
                                SERVICE_BOOKINGS.get(2).serviceId()
                        ),
                        List.of(
                                bookingTimeRangeFrom(SERVICE_BOOKINGS.get(2).bookings().get(0))
                        )
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideGetBookingTimeRangesTestCases")
    void getBookingTimeRanges(String name, GetBookingTimeRangesQuery query, List<TimeRange> expected) {
        // when
        List<TimeRange> result = REPOSITORY.getBookingTimeRanges(query);

        // then
        assertThat(result).isEqualTo(expected);
    }

    private static GetBookingTimeRangesQuery getGetBookingTimeRangesQuery(String date, ObjectId serviceId) {
        LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
        ServiceBookingIdentifier identifier = ServiceBookingIdentifier.Factory.create(localDate, serviceId);
        return new GetBookingTimeRangesQuery(identifier);
    }

    private static TimeRange bookingTimeRangeFrom(Booking booking) {
        return new TimeRange(
                booking.start(),
                booking.end()
        );
    }
}