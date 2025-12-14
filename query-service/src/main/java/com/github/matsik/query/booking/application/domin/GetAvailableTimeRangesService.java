package com.github.matsik.query.booking.application.domin;

import com.github.matsik.dto.BookingPartitionKey;
import com.github.matsik.dto.TimeRange;
import com.github.matsik.query.booking.application.port.in.GetAvailableTimeRangesQuery;
import com.github.matsik.query.booking.application.port.in.GetAvailableTimeRangesUseCase;
import com.github.matsik.query.booking.application.port.out.GetBookedTimeRangesPort;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetAvailableTimeRangesService implements GetAvailableTimeRangesUseCase {

    private final GetBookedTimeRangesPort getBookedTimeRangesPort;
    private final AvailableTimeRangesCalculator availableTimeRangesCalculator;

    @WithSpan(kind = SpanKind.SERVER)
    @Override
    public List<TimeRange> getAvailableTimeRanges(GetAvailableTimeRangesQuery query) {
        Span span = Span.current();
        setSpanAttributes(span, query);

        BookingPartitionKey key = query.bookingPartitionKey();
        List<TimeRange> unavailableTimeRanges = getBookedTimeRangesPort.getBookedTimeRanges(key.serviceId(), key.date());

        int serviceDuration = availableTimeRangesCalculator.getSystemServiceDuration(query.serviceDuration());

        return availableTimeRangesCalculator.getAvailableTimeRanges(unavailableTimeRanges, serviceDuration);
    }

    private void setSpanAttributes(Span span, GetAvailableTimeRangesQuery query) {
        BookingPartitionKey bookingPartitionKey = query.bookingPartitionKey();

        span.setAttribute(AttributeKey.stringKey("get_available_time_ranges_query.booking_partition_key.service_id"), bookingPartitionKey.serviceId().toString());
        span.setAttribute(AttributeKey.stringKey("get_available_time_ranges_query.booking_partition_key.date"), bookingPartitionKey.date().toString());
        span.setAttribute(AttributeKey.longKey("get_available_time_ranges_query.service_duration"), query.serviceDuration());
    }
}
