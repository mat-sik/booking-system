package com.github.matsik.query.request;

import com.github.matsik.mongo.model.ServiceBookingIdentifier;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bson.types.ObjectId;

import java.time.LocalDate;
import java.util.List;

@Getter
@Accessors(fluent = true)
public class GetBookingsRequest {

    private final List<LocalDate> dates;
    private final List<ObjectId> serviceIds;
    private final List<ObjectId> userIds;

    private GetBookingsRequest(List<LocalDate> dates, List<ObjectId> serviceIds, List<ObjectId> userIds) {
        this.dates = dates;
        this.serviceIds = serviceIds;
        this.userIds = userIds;
    }

    public static class Factory {
        public static GetBookingsRequest create(List<String> dates, List<String> serviceIds, List<String> userIds) {
            List<LocalDate> datesMapped = dates.stream()
                    .map(date -> LocalDate.parse(date, ServiceBookingIdentifier.FORMATTER))
                    .toList();

            List<ObjectId> serviceIdsMapped = serviceIds.stream()
                    .map(ObjectId::new)
                    .toList();

            List<ObjectId> userIdsMapped = userIds.stream()
                    .map(ObjectId::new)
                    .toList();

            return new GetBookingsRequest(datesMapped, serviceIdsMapped, userIdsMapped);
        }

        public static GetBookingsRequest newInstance(List<LocalDate> dates, List<ObjectId> serviceIds, List<ObjectId> userIds) {
            return new GetBookingsRequest(dates, serviceIds, userIds);
        }
    }
}
