package com.github.matsik.query.booking.repository;

import com.github.matsik.cassandra.model.BookingPartitionKey;
import com.github.matsik.query.booking.model.ServiceBooking;
import com.github.matsik.query.booking.model.UserBooking;
import com.github.matsik.query.booking.query.GetBookingQuery;
import com.github.matsik.query.booking.query.GetBookingTimeRangesQuery;
import com.github.matsik.query.booking.query.GetBookingsQuery;
import com.github.matsik.query.booking.service.TimeRange;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BookingRepositoryMongo implements BookingRepository {

    private final MongoTemplate template;

    @Override
    public Optional<UserBooking> getUserBooking(GetBookingQuery query) {
        AggregationOperation matchByDateAndServiceId = getMatchOperation(query.bookingPartitionKey());

        AggregationOperation unwindBookings = Aggregation.unwind("bookings");

        AggregationOperation projectBookingFields = Aggregation.project()
                .and("bookings._id").as("_id")
                .and("bookings.userId").as("userId")
                .and("bookings.start").as("start")
                .and("bookings.end").as("end");

        AggregationOperation matchBookingId = Aggregation.match(Criteria.where("_id").is(query.bookingId()));

        AggregationOperation projectNoId = Aggregation.project()
                .andExclude("_id");

        Aggregation aggregation = Aggregation.newAggregation(
                matchByDateAndServiceId,
                unwindBookings,
                projectBookingFields,
                matchBookingId,
                projectNoId
        );

        List<UserBooking> result = template.aggregate(aggregation, "service_bookings", UserBooking.class)
                .getMappedResults();
        return result.isEmpty() ? Optional.empty() : Optional.of(result.getFirst());
    }


    @Override
    public List<TimeRange> getBookingTimeRanges(GetBookingTimeRangesQuery query) {
        AggregationOperation matchByDateServiceId = getMatchOperation(query.bookingPartitionKey());

        AggregationOperation unwindBookings = Aggregation.unwind("$bookings");

        AggregationOperation projectTimeRange = Aggregation.project()
                .and("$bookings.start").as("start")
                .and("$bookings.end").as("end")
                .andExclude("_id");

        Aggregation aggregation = Aggregation.newAggregation(
                matchByDateServiceId,
                unwindBookings,
                projectTimeRange
        );

        return template.aggregate(aggregation, "service_bookings", TimeRange.class)
                .getMappedResults();
    }

    private static AggregationOperation getMatchOperation(BookingPartitionKey identifier) {
        return Aggregation.match(Criteria.where("date").is(identifier.date())
                .and("serviceId").is(identifier.serviceId()));
    }

    /*
     * Spring Boot data mongodb doesn't support projections that are more advanced than the most basic of use cases.
     */
    @Override
    public List<ServiceBooking> getBookings(GetBookingsQuery query) {
        Document matchByDatesAndServiceIds = matchByDatesAndServices(query.dates(), query.serviceIds());

        Document filterByUserIds = filterByUsers(query.userIds());

        BasicQuery basicQuery = new BasicQuery(matchByDatesAndServiceIds, filterByUserIds);

        return template.find(basicQuery, ServiceBooking.class);
    }

    private static Document matchByDatesAndServices(List<String> dates, List<ObjectId> serviceIds) {
        Document matchDoc = new Document();

        if (!dates.isEmpty()) {
            matchDoc.append("date", new Document("$in", dates));
        }

        if (!serviceIds.isEmpty()) {
            matchDoc.append("serviceId", new Document("$in", serviceIds));
        }

        return matchDoc;
    }

    private static Document filterByUsers(List<ObjectId> userIds) {
        Document filterDoc = new Document()
                .append("date", 1)
                .append("serviceId", 1);

        if (!userIds.isEmpty()) {
            filterDoc.append("bookings", getFilterDoc(userIds));
        } else {
            filterDoc.append("bookings", 1);
        }
        return filterDoc;
    }

    private static Document getFilterDoc(List<ObjectId> userIds) {
        return new Document("$filter", new Document()
                .append("input", "$bookings")
                .append("cond", new Document("$in", List.of("$$this.userId", userIds)))
        );
    }

}
