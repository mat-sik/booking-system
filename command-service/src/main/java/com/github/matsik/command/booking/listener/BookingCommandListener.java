package com.github.matsik.command.booking.listener;

import com.github.matsik.command.booking.command.CreateBookingCommand;
import com.github.matsik.command.booking.command.DeleteBookingCommand;
import com.github.matsik.command.booking.service.BookingService;
import com.github.matsik.kafka.task.CommandValue;
import com.github.matsik.kafka.task.CreateBookingCommandValue;
import com.github.matsik.kafka.task.DeleteBookingCommandValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Log
public class BookingCommandListener {

    private final BookingService service;

    @KafkaListener(topics = "bookings", groupId = "${kafka.groupId}", containerFactory = "kafkaListenerContainerFactory")
    public void listen(ConsumerRecord<LocalDate, CommandValue> record, Acknowledgment ack) {
        LocalDate key = record.key();
        CommandValue value = record.value();

        if (value instanceof CreateBookingCommandValue createBookingCommandValue) {
            CreateBookingCommand command = CreateBookingCommand.Factory.create(key, createBookingCommandValue);
            service.createBooking(command);
        } else if (value instanceof DeleteBookingCommandValue deleteBookingCommandValue) {
            DeleteBookingCommand command = DeleteBookingCommand.Factory.create(key, deleteBookingCommandValue);
            service.deleteBooking(command);
        } else {
            log.severe(String.format("Unexpected CommandValue: %s", value.toString()));
        }

        ack.acknowledge();
    }

}
