package com.exemple.gateway.core;

import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import okhttp3.mockwebserver.MockWebServer;

@SpringBootTest(classes = GatewayTestConfiguration.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
public class GatewayServerTestConfiguration {

    @Value("${api.port}")
    private int apiPort;

    @Value("${authorization.port}")
    private int authorizationPort;

    @LocalServerPort
    protected int localPort;

    protected MockWebServer apiServer;

    protected MockWebServer authorizationServer;

    @BeforeAll
    void apiServer() throws IOException {
        this.apiServer = new MockWebServer();
        this.apiServer.start(apiPort);
    }

    @BeforeAll
    void authorizationServer() throws IOException {
        this.authorizationServer = new MockWebServer();
        this.authorizationServer.start(authorizationPort);
    }

    @AfterAll
    void closeMockServer() throws IOException {
        this.apiServer.shutdown();
        this.authorizationServer.shutdown();
    }

}
