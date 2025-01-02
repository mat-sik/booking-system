package com.github.matsik.command.booking.listener;

import com.github.matsik.command.booking.command.CreateBookingCommand;
import com.github.matsik.command.booking.command.DeleteBookingCommand;
import com.github.matsik.command.booking.service.BookingService;
import com.github.matsik.kafka.task.CommandValue;
import com.github.matsik.kafka.task.CreateBookingCommandValue;
import com.github.matsik.kafka.task.DeleteBookingCommandValue;
import com.github.matsik.mongo.model.ServiceBookingIdentifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Log
public class BookingCommandListener {

    private final BookingService service;

    @KafkaListener(topics = "bookings", groupId = "${kafka.groupId}", containerFactory = "kafkaListenerContainerFactory")
    public void listen(List<ConsumerRecord<ServiceBookingIdentifier, CommandValue>> records, Acknowledgment ack) {
        records.forEach(this::processRecord);
        ack.acknowledge();
    }

    private void processRecord(ConsumerRecord<ServiceBookingIdentifier, CommandValue> record) {
        ServiceBookingIdentifier key = record.key();
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
    }

}
