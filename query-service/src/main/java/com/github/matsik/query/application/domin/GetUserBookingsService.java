package com.github.matsik.query.application.domin;

import com.github.matsik.query.application.port.in.GetFirstUserBookingsQuery;
import com.github.matsik.query.application.port.in.GetNextUserBookingsQuery;
import com.github.matsik.query.application.port.in.GetUserBookingsQuery;
import com.github.matsik.query.application.port.in.GetUserBookingsUseCase;
import com.github.matsik.query.application.port.out.GetFirstUserBookingsPort;
import com.github.matsik.query.application.port.out.GetNextUserBookingsPort;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetUserBookingsService implements GetUserBookingsUseCase {

    private final GetFirstUserBookingsPort getFirstUserBookingsPort;
    private final GetNextUserBookingsPort getNextUserBookingsPort;

    @Override
    public List<UserBooking> getUserBookings(GetUserBookingsQuery query) {
        return switch (query) {
            case GetFirstUserBookingsQuery first -> getFirstUserBookings(first);
            case GetNextUserBookingsQuery next -> getNextUserBookings(next);
        };
    }

    @WithSpan(kind = SpanKind.SERVER)
    private List<UserBooking> getFirstUserBookings(GetFirstUserBookingsQuery query) {
        Span span = Span.current();
        setSpanAttributes(span, query);

        return getFirstUserBookingsPort.getFirstUserBookings(query.userId(), query.limit());
    }

    private void setSpanAttributes(Span span, GetFirstUserBookingsQuery query) {
        span.setAttribute(AttributeKey.stringKey("get_first_user_bookings_query.user_id"), query.userId().toString());
        span.setAttribute(AttributeKey.longKey("get_first_user_bookings_query.limit"), query.limit());
    }

    @WithSpan(kind = SpanKind.SERVER)
    private List<UserBooking> getNextUserBookings(GetNextUserBookingsQuery query) {
        Span span = Span.current();
        setSpanAttributes(span, query);

        return getNextUserBookingsPort.getNextUserBookings(
                query.userId(),
                query.cursorServiceId(),
                query.cursorDate(),
                query.cursorBookingId(),
                query.limit()
        );
    }

    private void setSpanAttributes(Span span, GetNextUserBookingsQuery query) {
        span.setAttribute(AttributeKey.stringKey("get_next_user_bookings_query.user_id"), query.userId().toString());
        span.setAttribute(AttributeKey.stringKey("get_next_user_bookings_query.cursor_service_id"), query.cursorServiceId().toString());
        span.setAttribute(AttributeKey.stringKey("get_next_user_bookings_query.cursor_date"), query.cursorDate().toString());
        span.setAttribute(AttributeKey.stringKey("get_next_user_bookings_query.cursor_booking_id"), query.cursorBookingId().toString());
        span.setAttribute(AttributeKey.longKey("get_next_user_bookings_query.limit"), query.limit());
    }
}
