package com.github.matsik.command.config.cassandra.mapper.booking;

import com.datastax.oss.driver.api.core.CqlSession;
import com.github.matsik.command.booking.repository.BookingRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BookingMapperConfiguration {

    @Bean
    public BookingMapper bookingMapper(CqlSession cqlSession) {
        return new BookingMapperBuilder(cqlSession).build();
    }

    @Bean
    public BookingRepository bookingRepository(BookingMapper bookingMapper) {
        return bookingMapper.bookingRepository();
    }

}
