package com.github.matsik.query.booking.grpc;

import com.github.matsik.dto.TimeRange;
import com.github.matsik.query.booking.query.GetAvailableTimeRangesQuery;
import com.github.matsik.query.booking.query.GetUserBookingQuery;
import com.github.matsik.query.booking.query.GetUserBookingsQuery;
import com.github.matsik.query.booking.repository.projection.UserBooking;
import com.github.matsik.query.booking.service.BookingService;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import lombok.RequiredArgsConstructor;
import org.springframework.grpc.server.service.GrpcService;

import java.util.List;

import static com.github.matsik.query.metrics.MetricsRecorder.recordMetrics;

@GrpcService
@RequiredArgsConstructor
public class QueryServiceGrpcImpl extends QueryServiceGrpc.QueryServiceImplBase {

    private final BookingService bookingService;
    private final GrpcMapper grpcMapper;

    private final LongCounter requestCounter;
    private final DoubleHistogram requestHistogram;

    public void listAvailableTimeRanges(
            ListAvailableTimeRangesRequest request,
            StreamObserver<ListAvailableTimeRangesResponse> responseObserver
    ) {
        recordMetrics(requestCounter, requestHistogram, () -> {
            GetAvailableTimeRangesQuery query = grpcMapper.getAvailableTimeRangesQuery(request);

            List<TimeRange> availableTimeRanges = bookingService.getAvailableTimeRanges(query);

            responseObserver.onNext(grpcMapper.listAvailableTimeRangesResponse(availableTimeRanges));
            responseObserver.onCompleted();
        }, "list_available_time_ranges");
    }

    @Override
    public void getUserBookingTimeRange(
            GetUserBookingTimeRangeRequest request,
            StreamObserver<GetUserBookingTimeRangeResponse> responseObserver
    ) {
        recordMetrics(requestCounter, requestHistogram, () -> {
            GetUserBookingQuery query = grpcMapper.getUserBookingQuery(request);

            TimeRange userBookingTimeRange = bookingService.getUserBookingTimeRange(query);

            responseObserver.onNext(grpcMapper.getUserBookingTimeRangeResponse(userBookingTimeRange));
            responseObserver.onCompleted();
        }, "get_user_booking_time_range");
    }

    @Override
    public void listUserBookings(
            ListUserBookingsRequest request,
            StreamObserver<ListUserBookingsResponse> responseObserver
    ) {
        recordMetrics(requestCounter, requestHistogram, () -> {
            GetUserBookingsQuery query = grpcMapper.GetUserBookingsQuery(request);

            List<UserBooking> userBookings = bookingService.getUserBookings(query);

            responseObserver.onNext(grpcMapper.listUserBookingsResponse(userBookings));
            responseObserver.onCompleted();
        }, "list_user_bookings");
    }
}
