package com.github.matsik.booking.client.query;

import com.github.matsik.booking.controller.response.TimeRangeResponse;
import com.github.matsik.booking.controller.response.UserBookingResponse;
import com.github.matsik.query.booking.grpc.TimeRange;
import com.github.matsik.query.booking.grpc.UserBooking;
import org.springframework.stereotype.Component;

import static com.github.matsik.query.grpc.GrpcMapper.localDate;
import static com.github.matsik.query.grpc.GrpcMapper.uuid;

@Component
public class GrpcMapper {

    public TimeRangeResponse timeRangeResponse(TimeRange timeRange) {
        return new TimeRangeResponse(timeRange.getStart(), timeRange.getEnd());
    }

    public UserBookingResponse userBookingResponse(UserBooking userBooking) {
        return new UserBookingResponse(
                uuid(userBooking.getServiceId()),
                localDate(userBooking.getDate()),
                uuid(userBooking.getBookingId()),
                userBooking.getStart(),
                userBooking.getEnd()
        );
    }
}
