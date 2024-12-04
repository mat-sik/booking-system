package com.github.matsik.query.booking.repository;

import com.github.matsik.query.booking.model.UserBooking;
import com.github.matsik.query.booking.query.GetBooking;
import com.github.matsik.query.booking.query.ServiceBookingIdentifier;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BookingRepository {

    private final MongoTemplate template;

    public Optional<UserBooking> getUserBooking(GetBooking request) {
        AggregationOperation matchDateServiceId = getMatchOperation(request.serviceBookingIdentifier());

        AggregationOperation unwindBookings = Aggregation.unwind("bookings");

        AggregationOperation projectBookingFields = Aggregation.project()
                .and("bookings._id").as("_id")
                .and("bookings.userId").as("userId")
                .and("bookings.start").as("start")
                .and("bookings.end").as("end");

        AggregationOperation matchBookingId = Aggregation.match(Criteria.where("_id").is(request.bookingId()));

        AggregationOperation projectNoId = Aggregation.project()
                .andExclude("_id");

        Aggregation aggregation = Aggregation.newAggregation(
                matchDateServiceId,
                unwindBookings,
                projectBookingFields,
                matchBookingId,
                projectNoId
        );

        List<UserBooking> result = template.aggregate(aggregation, "service_bookings", UserBooking.class)
                .getMappedResults();
        return result.isEmpty() ? Optional.empty() : Optional.of(result.getFirst());
    }

    private static AggregationOperation getMatchOperation(ServiceBookingIdentifier identifier) {
        return Aggregation.match(Criteria.where("date").is(identifier.date())
                .and("serviceId").is(identifier.serviceId()));
    }

}
