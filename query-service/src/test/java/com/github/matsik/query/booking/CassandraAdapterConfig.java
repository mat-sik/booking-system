package com.github.matsik.query.booking;

import com.github.matsik.query.booking.adapter.out.BookingPersistenceAdapter;
import com.github.matsik.query.booking.application.domin.AvailableTimeRangesCalculator;
import com.github.matsik.query.booking.application.domin.GetAvailableTimeRangesService;
import com.github.matsik.query.booking.application.domin.GetUserBookingService;
import com.github.matsik.query.booking.application.domin.GetUserBookingsService;
import com.github.matsik.query.config.cassandra.client.CassandraClientConfiguration;
import com.github.matsik.query.config.cassandra.client.CassandraClientProperties;
import com.github.matsik.query.config.cassandra.mapper.booking.BookingMapperConfiguration;
import com.github.matsik.query.config.otel.OtelConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(value = {
        CassandraClientConfiguration.class,
        BookingMapperConfiguration.class,
        BookingPersistenceAdapter.class,
        AvailableTimeRangesCalculator.class,
        GetAvailableTimeRangesService.class,
        GetUserBookingService.class,
        GetUserBookingsService.class,
        OtelConfiguration.class
})
@EnableConfigurationProperties(CassandraClientProperties.class)
public class CassandraAdapterConfig {
}
