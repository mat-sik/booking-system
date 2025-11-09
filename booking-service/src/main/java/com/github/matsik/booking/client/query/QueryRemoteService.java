package com.github.matsik.booking.client.query;

import com.github.matsik.booking.controller.response.TimeRangeResponse;
import com.github.matsik.booking.controller.response.UserBookingResponse;
import com.github.matsik.query.booking.grpc.GetUserBookingTimeRangeRequest;
import com.github.matsik.query.booking.grpc.GetUserBookingTimeRangeResponse;
import com.github.matsik.query.booking.grpc.ListAvailableTimeRangesRequest;
import com.github.matsik.query.booking.grpc.ListAvailableTimeRangesResponse;
import com.github.matsik.query.booking.grpc.ListUserBookingsRequest;
import com.github.matsik.query.booking.grpc.ListUserBookingsResponse;
import com.github.matsik.query.booking.grpc.QueryServiceGrpc;
import io.grpc.StatusException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.github.matsik.query.grpc.GrpcMapper.grpcDate;

@Service
@RequiredArgsConstructor
public class QueryRemoteService {

    private final QueryServiceGrpc.QueryServiceBlockingV2Stub queryServiceStub;
    private final GrpcMapper grpcMapper;

    public List<TimeRangeResponse> getAvailableTimeRanges(UUID serviceId, LocalDate date, int serviceDuration) {
        ListAvailableTimeRangesRequest request = ListAvailableTimeRangesRequest.newBuilder()
                .setServiceId(serviceId.toString())
                .setDate(grpcDate(date))
                .setServiceDuration(serviceDuration)
                .build();

        try {
            ListAvailableTimeRangesResponse response = queryServiceStub.listAvailableTimeRanges(request);
            return response.getTimeRangesList().stream()
                    .map(grpcMapper::timeRangeResponse)
                    .toList();
        } catch (StatusException ex) {
            throw new RuntimeException(ex);
        }
    }

    public TimeRangeResponse getUserBookingTimeRange(
            UUID serviceId,
            LocalDate date,
            UUID userId,
            UUID bookingId
    ) {
        GetUserBookingTimeRangeRequest request = GetUserBookingTimeRangeRequest.newBuilder()
                .setServiceId(serviceId.toString())
                .setDate(grpcDate(date))
                .setUserId(userId.toString())
                .setBookingId(bookingId.toString())
                .build();

        try {
            GetUserBookingTimeRangeResponse response = queryServiceStub.getUserBookingTimeRange(request);
            return grpcMapper.timeRangeResponse(response.getTimeRange());
        } catch (StatusException ex) {
            throw new RuntimeException(ex);
        }
    }

    public List<UserBookingResponse> getFirstUserBookings(UUID userId, int limit) {
        ListUserBookingsRequest request = ListUserBookingsRequest.newBuilder()
                .setUserId(userId.toString())
                .setLimit(limit)
                .build();

        return listUserBookings(request);
    }

    public List<UserBookingResponse> getNextUserBookings(
            UUID userId,
            UUID cursorServiceId,
            LocalDate cursorDate,
            UUID cursorBookingId,
            int limit
    ) {
        ListUserBookingsRequest request = ListUserBookingsRequest.newBuilder()
                .setUserId(userId.toString())
                .setLimit(limit)
                .setCursorServiceId(cursorServiceId.toString())
                .setCursorDate(grpcDate(cursorDate))
                .setCursorBookingId(cursorBookingId.toString())
                .build();

        return listUserBookings(request);
    }

    private List<UserBookingResponse> listUserBookings(ListUserBookingsRequest request) {
        try {
            ListUserBookingsResponse response = queryServiceStub.listUserBookings(request);
            return response.getUserBookingsList().stream()
                    .map(grpcMapper::userBookingResponse)
                    .toList();
        } catch (StatusException ex) {
            throw new RuntimeException(ex);
        }
    }

}
