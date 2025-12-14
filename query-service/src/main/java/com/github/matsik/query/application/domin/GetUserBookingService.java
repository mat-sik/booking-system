package com.github.matsik.query.application.domin;

import com.github.matsik.dto.BookingPartitionKey;
import com.github.matsik.dto.TimeRange;
import com.github.matsik.query.application.port.in.GetUserBookingQuery;
import com.github.matsik.query.application.port.in.GetUserBookingUseCase;
import com.github.matsik.query.application.port.out.GetUserBookingTimeRangePort;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GetUserBookingService implements GetUserBookingUseCase {

    private final GetUserBookingTimeRangePort getUserBookingTimeRangePort;

    @WithSpan(kind = SpanKind.SERVER)
    @Override
    public Optional<TimeRange> getUserBookingTimeRange(GetUserBookingQuery query) {
        Span span = Span.current();
        setSpanAttributes(span, query);

        BookingPartitionKey key = query.bookingPartitionKey();

        return getUserBookingTimeRangePort.getUserBookingTimeRange(query.userId(), key.serviceId(), key.date(), query.bookingId());
    }

    private void setSpanAttributes(Span span, GetUserBookingQuery query) {
        BookingPartitionKey bookingPartitionKey = query.bookingPartitionKey();

        span.setAttribute(AttributeKey.stringKey("get_user_booking_query.booking_partition_key.service_id"), bookingPartitionKey.serviceId().toString());
        span.setAttribute(AttributeKey.stringKey("get_user_booking_query.booking_partition_key.date"), bookingPartitionKey.date().toString());
        span.setAttribute(AttributeKey.stringKey("get_user_booking_query.user_id"), query.userId().toString());
        span.setAttribute(AttributeKey.stringKey("get_user_booking_query.booking_id"), query.bookingId().toString());
    }
}
