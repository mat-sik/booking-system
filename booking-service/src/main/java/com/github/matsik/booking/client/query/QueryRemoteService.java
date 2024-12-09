package com.github.matsik.booking.client.query;

import com.github.matsik.query.response.ServiceBookingResponse;
import com.github.matsik.query.response.TimeRangeResponse;
import com.github.matsik.query.response.UserBookingResponse;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QueryRemoteService {

    private final QueryClient client;

    public List<TimeRangeResponse> getAvailableTimeRanges(
            LocalDate date,
            ObjectId serviceId,
            int serviceDuration
    ) {
        return client.getAvailableTimeRanges(date, serviceId, serviceDuration);
    }

    public UserBookingResponse getUserBooking(
            LocalDate date,
            ObjectId serviceId,
            ObjectId bookingId
    ) {
        return client.getUserBooking(date, serviceId, bookingId);
    }

    public List<ServiceBookingResponse> getBookings(
            List<LocalDate> dates,
            List<ObjectId> serviceIds,
            List<ObjectId> userIds
    ) {
        return client.getBookings(dates, serviceIds, userIds);
    }
}
