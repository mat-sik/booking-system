package com.github.matsik.query.booking.grpc;

import com.github.matsik.dto.TimeRange;
import com.github.matsik.query.booking.query.GetAvailableTimeRangesQuery;
import com.github.matsik.query.booking.query.GetUserBookingQuery;
import com.github.matsik.query.booking.query.GetUserBookingsQuery;
import com.github.matsik.query.booking.repository.projection.UserBooking;
import com.github.matsik.query.booking.service.BookingService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.grpc.server.service.GrpcService;

import java.util.List;

@RequiredArgsConstructor
@GrpcService
public class QueryServiceGrpcImpl extends QueryServiceGrpc.QueryServiceImplBase {

    private final BookingService bookingService;
    private final GrpcMapper grpcMapper;

    public void listAvailableTimeRanges(
            ListAvailableTimeRangesRequest request,
            StreamObserver<ListAvailableTimeRangesResponse> responseObserver
    ) {
        GetAvailableTimeRangesQuery query = grpcMapper.getAvailableTimeRangesQuery(request);

        List<TimeRange> availableTimeRanges = bookingService.getAvailableTimeRanges(query);

        responseObserver.onNext(grpcMapper.listAvailableTimeRangesResponse(availableTimeRanges));
        responseObserver.onCompleted();
    }

    @Override
    public void getUserBookingTimeRange(
            GetUserBookingTimeRangeRequest request,
            StreamObserver<GetUserBookingTimeRangeResponse> responseObserver
    ) {
        GetUserBookingQuery query = grpcMapper.getUserBookingQuery(request);

        TimeRange userBookingTimeRange = bookingService.getUserBookingTimeRange(query);

        responseObserver.onNext(grpcMapper.getUserBookingTimeRangeResponse(userBookingTimeRange));
        responseObserver.onCompleted();
    }

    @Override
    public void listUserBookings(
            ListUserBookingsRequest request,
            StreamObserver<ListUserBookingsResponse> responseObserver
    ) {
        GetUserBookingsQuery query = grpcMapper.GetUserBookingsQuery(request);

        List<UserBooking> userBookings = bookingService.getUserBookings(query);

        responseObserver.onNext(grpcMapper.listUserBookingsResponse(userBookings));
        responseObserver.onCompleted();
    }
}
