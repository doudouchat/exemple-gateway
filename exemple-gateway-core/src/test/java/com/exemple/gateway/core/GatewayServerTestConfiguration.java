package com.exemple.gateway.core;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = GatewayTestConfiguration.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
public class GatewayServerTestConfiguration {

    static {
        System.setProperty("mockserver.logLevel", "DEBUG");
    }

    @Value("${api.port}")
    private int apiPort;

    @Value("${authorization.port}")
    private int authorizationPort;

    private ClientAndServer apiServer;

    protected MockServerClient apiClient;

    private ClientAndServer authorizationServer;

    protected MockServerClient authorizationClient;

    @BeforeAll
    public final void apiServer() {
        this.apiServer = ClientAndServer.startClientAndServer(apiPort);
        this.apiClient = new MockServerClient("localhost", apiPort);
    }

    @BeforeAll
    public final void authorizationServer() {
        this.authorizationServer = ClientAndServer.startClientAndServer(authorizationPort);
        this.authorizationClient = new MockServerClient("localhost", authorizationPort);
    }

    @AfterAll
    public final void closeMockServer() {

        this.apiServer.close();
        this.apiServer.hasStopped();
        this.authorizationServer.close();
        this.authorizationServer.hasStopped();
    }

}
