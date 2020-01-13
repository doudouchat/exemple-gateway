package com.exemple.gateway.core;

import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
public class GatewayServerTestConfiguration {

    static {
        System.setProperty("mockserver.logLevel", "DEBUG");
    }

    @Value("${api.port}")
    private int apiPort;

    @Value("${authorization.port}")
    private int authorizationPort;

    @Bean(destroyMethod = "stop")
    public ClientAndServer apiServer() {

        return ClientAndServer.startClientAndServer(apiPort);
    }

    @Bean(destroyMethod = "stop")
    public ClientAndServer authorizationServer() {
        return ClientAndServer.startClientAndServer(authorizationPort);
    }

    @Bean(destroyMethod = "stop")
    @DependsOn("apiServer")
    public MockServerClient apiClient() {

        return new MockServerClient("localhost", apiPort);
    }

    @Bean(destroyMethod = "stop")
    @DependsOn("authorizationServer")
    public MockServerClient authorizationClient() {

        return new MockServerClient("localhost", authorizationPort);
    }

}
