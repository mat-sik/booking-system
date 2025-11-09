package com.github.matsik.booking.config.grpc;

import com.github.matsik.query.booking.grpc.QueryServiceGrpc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

@Configuration
public class QueryRemoteServiceClientConfiguration {

    @Bean
    public QueryServiceGrpc.QueryServiceBlockingV2Stub queryServiceStub(
            GrpcChannelFactory grpcChannelFactory,
            QueryRemoteServiceClientProperties queryRemoteServiceClientProperties
    ) {
        return QueryServiceGrpc.newBlockingV2Stub(grpcChannelFactory.createChannel(queryRemoteServiceClientProperties.address()));
    }

}
