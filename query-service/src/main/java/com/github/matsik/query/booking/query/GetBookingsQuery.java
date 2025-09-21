package com.github.matsik.query.booking.query;

import com.github.matsik.cassandra.model.BookingPartitionKey;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bson.types.ObjectId;

import java.time.LocalDate;
import java.util.List;

@Getter
@Accessors(fluent = true)
@EqualsAndHashCode
public class GetBookingsQuery {

    private final List<String> dates;
    private final List<ObjectId> serviceIds;
    private final List<ObjectId> userIds;

    public GetBookingsQuery(List<LocalDate> dates, List<ObjectId> serviceIds, List<ObjectId> userIds) {
        this.dates = dates.stream()
                .map(date -> date.format(BookingPartitionKey.FORMATTER))
                .toList();

        this.serviceIds = serviceIds;
        this.userIds = userIds;
    }
}
