package com.github.matsik.command.booking.listener;

import com.github.matsik.command.booking.command.CreateBookingCommand;
import com.github.matsik.command.booking.command.DeleteBookingCommand;
import com.github.matsik.command.booking.service.BookingService;
import com.github.matsik.dto.BookingPartitionKey;
import com.github.matsik.kafka.task.CommandValue;
import com.github.matsik.kafka.task.CreateBookingCommandValue;
import com.github.matsik.kafka.task.DeleteBookingCommandValue;
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
    public void listen(List<ConsumerRecord<BookingPartitionKey, CommandValue>> records, Acknowledgment ack) {
        records.forEach(this::processRecord);
        ack.acknowledge();
    }

    private void processRecord(ConsumerRecord<BookingPartitionKey, CommandValue> record) {
        BookingPartitionKey key = record.key();
        CommandValue value = record.value();

        switch (value) {
            case CreateBookingCommandValue create -> {
                CreateBookingCommand command = CreateBookingCommand.of(key, create);
                service.createBooking(command);
            }
            case DeleteBookingCommandValue delete -> {
                DeleteBookingCommand command = DeleteBookingCommand.of(key, delete);
                service.deleteBooking(command);
            }
        }
    }

}
