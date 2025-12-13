package com.github.matsik.command.kafka;

import com.github.matsik.command.adapter.out.cassandra.BookingCache;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.common.TopicPartition;

import java.util.Collection;

@RequiredArgsConstructor
public class BookingConsumerRebalanceListener implements ConsumerRebalanceListener {

    private final BookingCache bookingCache;

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> collection) {
        bookingCache.clear();
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> collection) {

    }

}
