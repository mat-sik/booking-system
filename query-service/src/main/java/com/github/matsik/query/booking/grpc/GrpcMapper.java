package com.github.matsik.query.booking.grpc;

import com.github.matsik.dto.TimeRange;
import com.github.matsik.query.booking.query.GetAvailableTimeRangesQuery;
import com.github.matsik.query.booking.query.GetFirstUserBookingsQuery;
import com.github.matsik.query.booking.query.GetNextUserBookingsQuery;
import com.github.matsik.query.booking.query.GetUserBookingQuery;
import com.github.matsik.query.booking.query.GetUserBookingsQuery;
import com.github.matsik.query.booking.repository.projection.UserBooking;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.github.matsik.query.grpc.GrpcMapper.grpcDate;
import static com.github.matsik.query.grpc.GrpcMapper.localDate;
import static com.github.matsik.query.grpc.GrpcMapper.uuid;

@Component
public class GrpcMapper {

    public GetAvailableTimeRangesQuery getAvailableTimeRangesQuery(ListAvailableTimeRangesRequest request) {
        return GetAvailableTimeRangesQuery.of(
                uuid(request.getServiceId()),
                localDate(request.getDate()),
                request.getServiceDuration()
        );
    }

    public ListAvailableTimeRangesResponse listAvailableTimeRangesResponse(List<TimeRange> availableTimeRanges) {
        return ListAvailableTimeRangesResponse.newBuilder()
                .addAllTimeRanges(availableTimeRanges.stream()
                        .map(this::grpcTimeRange)
                        .toList()
                )
                .build();
    }

    public GetUserBookingQuery getUserBookingQuery(GetUserBookingTimeRangeRequest request) {
        return GetUserBookingQuery.of(
                uuid(request.getServiceId()),
                localDate(request.getDate()),
                uuid(request.getUserId()),
                uuid(request.getBookingId())
        );
    }

    public GetUserBookingTimeRangeResponse getUserBookingTimeRangeResponse(TimeRange userBookingTimeRange) {
        return GetUserBookingTimeRangeResponse.newBuilder()
                .setTimeRange(grpcTimeRange(userBookingTimeRange))
                .build();
    }

    private com.github.matsik.query.booking.grpc.TimeRange grpcTimeRange(TimeRange timeRange) {
        return com.github.matsik.query.booking.grpc.TimeRange.newBuilder()
                .setStart(timeRange.start().minuteOfDay())
                .setEnd(timeRange.end().minuteOfDay())
                .build();
    }

    public GetUserBookingsQuery GetUserBookingsQuery(ListUserBookingsRequest request) {
        if (!request.getCursorServiceId().isEmpty() && request.hasCursorDate() && !request.getCursorBookingId().isEmpty()) {
            return new GetNextUserBookingsQuery(
                    uuid(request.getUserId()),
                    uuid(request.getCursorServiceId()),
                    localDate(request.getCursorDate()),
                    uuid(request.getCursorBookingId()),
                    request.getLimit()
            );
        }
        return new GetFirstUserBookingsQuery(uuid(request.getUserId()), request.getLimit());
    }

    public ListUserBookingsResponse listUserBookingsResponse(List<UserBooking> userBookings) {
        return ListUserBookingsResponse.newBuilder()
                .addAllUserBookings(userBookings.stream()
                        .map(this::grpcUserBooking)
                        .toList()
                )
                .build();
    }

    private com.github.matsik.query.booking.grpc.UserBooking grpcUserBooking(UserBooking userBooking) {
        return com.github.matsik.query.booking.grpc.UserBooking.newBuilder()
                .setServiceId(userBooking.serviceId().toString())
                .setDate(grpcDate(userBooking.date()))
                .setBookingId(userBooking.bookingId().toString())
                .setStart(userBooking.timeRange().start().minuteOfDay())
                .setEnd(userBooking.timeRange().end().minuteOfDay())
                .build();
    }
}
