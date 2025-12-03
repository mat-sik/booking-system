package com.github.matsik.command.kafka;

import com.github.matsik.dto.BookingPartitionKey;
import com.github.matsik.kafka.task.CommandValue;
import org.apache.kafka.clients.consumer.ConsumerRecords;

public interface RecordsHandler {
    void onRecords(ConsumerRecords<BookingPartitionKey, CommandValue> records);
}
