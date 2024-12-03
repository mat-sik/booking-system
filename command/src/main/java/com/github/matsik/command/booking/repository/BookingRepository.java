package com.github.matsik.command.booking.repository;

import com.github.matsik.command.booking.command.CreateBooking;
import com.github.matsik.command.booking.command.CreateServiceBooking;
import com.github.matsik.command.booking.model.Booking;
import com.github.matsik.command.booking.model.ServiceBooking;
import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.MongoExpression;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BookingRepository {

    private final MongoTemplate template;

    public UpdateResult createBooking(CreateBooking request) {
        CreateServiceBooking createServiceBooking = request.createServiceBooking();

        ensureServiceBookingExists(createServiceBooking);

        Criteria matchCriteria = getMatchCriteria(createServiceBooking.date(), createServiceBooking.serviceId());
        Criteria overlapCriteria = getOverlapCriteria(request.start(), request.end());

        Query query = new Query(matchCriteria)
                .addCriteria(overlapCriteria);

        Booking booking = new Booking(new ObjectId(), request.userId(), request.start(), request.end());
        Update update = new Update()
                .push("bookings", booking);

        return template.updateFirst(query, update, ServiceBooking.class);
    }

    private UpdateResult ensureServiceBookingExists(CreateServiceBooking request) {
        Criteria matchCriteria = getMatchCriteria(request.date(), request.serviceId());

        Query query = new Query(matchCriteria);

        Update update = new Update()
                .setOnInsert("date", request.date())
                .setOnInsert("serviceId", request.serviceId())
                .setOnInsert("bookings", List.of());

        return template.upsert(query, update, ServiceBooking.class);
    }

    private static Criteria getMatchCriteria(String date, ObjectId serviceId) {
        return Criteria.where("date").is(date)
                .and("serviceId").is(serviceId);
    }

    private static Criteria getOverlapCriteria(int start, int end) {
        Document docCriteria = new Document("$not",
                new Document("$anyElementTrue",
                        new Document("$map",
                                new Document("input", "$bookings")
                                        .append("in",
                                                new Document("$and",
                                                        List.of(
                                                                new Document("$lt", List.of(start, "$$this.end")),
                                                                new Document("$gt", List.of(end, "$$this.start"))
                                                        )
                                                )
                                        )
                        )
                )
        );

        return Criteria.expr(MongoExpression.create(docCriteria.toJson()));
    }
}
