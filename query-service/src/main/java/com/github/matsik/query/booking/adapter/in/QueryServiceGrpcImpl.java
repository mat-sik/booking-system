package com.github.matsik.query.booking.adapter.in;

import com.github.matsik.dto.TimeRange;
import com.github.matsik.query.booking.application.port.in.GetAvailableTimeRangesQuery;
import com.github.matsik.query.booking.application.port.in.GetAvailableTimeRangesUseCase;
import com.github.matsik.query.booking.application.port.in.GetUserBookingQuery;
import com.github.matsik.query.booking.application.port.in.GetUserBookingUseCase;
import com.github.matsik.query.booking.application.port.in.GetUserBookingsQuery;
import com.github.matsik.query.booking.application.port.in.GetUserBookingsUseCase;
import com.github.matsik.query.booking.application.domin.UserBooking;
import com.github.matsik.query.booking.grpc.GetUserBookingTimeRangeRequest;
import com.github.matsik.query.booking.grpc.GetUserBookingTimeRangeResponse;
import com.github.matsik.query.booking.grpc.ListAvailableTimeRangesRequest;
import com.github.matsik.query.booking.grpc.ListAvailableTimeRangesResponse;
import com.github.matsik.query.booking.grpc.ListUserBookingsRequest;
import com.github.matsik.query.booking.grpc.ListUserBookingsResponse;
import com.github.matsik.query.booking.grpc.QueryServiceGrpc;
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

    private final GetAvailableTimeRangesUseCase getAvailableTimeRangesUseCase;
    private final GetUserBookingsUseCase getUserBookingsUseCase;
    private final GetUserBookingUseCase getUserBookingUseCase;

    private final GrpcMapper grpcMapper;

    private final LongCounter requestCounter;
    private final DoubleHistogram requestHistogram;

    public void listAvailableTimeRanges(
            ListAvailableTimeRangesRequest request,
            StreamObserver<ListAvailableTimeRangesResponse> responseObserver
    ) {
        recordMetrics(requestCounter, requestHistogram, () -> {
            GetAvailableTimeRangesQuery query = grpcMapper.getAvailableTimeRangesQuery(request);

            List<TimeRange> availableTimeRanges = getAvailableTimeRangesUseCase.getAvailableTimeRanges(query);

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

            TimeRange userBookingTimeRange = getUserBookingUseCase.getUserBookingTimeRange(query)
                    .orElseThrow(() -> new UserBookingNotFoundException(query));

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

            List<UserBooking> userBookings = getUserBookingsUseCase.getUserBookings(query);

            responseObserver.onNext(grpcMapper.listUserBookingsResponse(userBookings));
            responseObserver.onCompleted();
        }, "list_user_bookings");
    }
}
