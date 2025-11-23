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
import io.opentelemetry.api.metrics.Meter;
import lombok.RequiredArgsConstructor;
import org.springframework.grpc.server.service.GrpcService;

import java.util.List;

@RequiredArgsConstructor
@GrpcService
public class QueryServiceGrpcImpl extends QueryServiceGrpc.QueryServiceImplBase {

    private final BookingService bookingService;
    private final GrpcMapper grpcMapper;
    private final Meter meter;

    public void listAvailableTimeRanges(
            ListAvailableTimeRangesRequest request,
            StreamObserver<ListAvailableTimeRangesResponse> responseObserver
    ) {
        recordMetrics(() -> {
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
        recordMetrics(() -> {
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
        recordMetrics(() -> {
            GetUserBookingsQuery query = grpcMapper.GetUserBookingsQuery(request);

            List<UserBooking> userBookings = bookingService.getUserBookings(query);

            responseObserver.onNext(grpcMapper.listUserBookingsResponse(userBookings));
            responseObserver.onCompleted();
        }, "list_user_bookings");
    }

    private void recordMetrics(Runnable operation, String requestName) {
        long startTime = System.nanoTime();
        operation.run();
        long duration = System.nanoTime() - startTime;

        recordDurationAndIncrementCounter(meter, duration, requestName);
    }

    private void recordDurationAndIncrementCounter(Meter meter, long durationNs, String requestName) {
        LongCounter counter = meter.counterBuilder(String.format("%s.requests", requestName))
                .setDescription(String.format("Total %s requests", requestName))
                .setUnit("requests")
                .build();

        DoubleHistogram histogram = meter.histogramBuilder(String.format("%s.duration", requestName))
                .setDescription(String.format("Duration of handling a %s request", requestName))
                .setUnit("ms")
                .build();

        counter.add(1L);
        histogram.record(durationNs / 1_000_000.0);
    }
}
