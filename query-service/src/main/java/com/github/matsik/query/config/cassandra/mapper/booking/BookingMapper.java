package com.github.matsik.query.config.cassandra.mapper.booking;

import com.datastax.oss.driver.api.mapper.annotations.DaoFactory;
import com.datastax.oss.driver.api.mapper.annotations.Mapper;
import com.github.matsik.query.booking.repository.BookingRepository;

@Mapper
public interface BookingMapper {

    @DaoFactory
    BookingRepository bookingRepository();

}
