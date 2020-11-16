package com.exemple.gateway.core;

import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

@SpringBootTest(classes = GatewayTestConfiguration.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class GatewayServerTestConfiguration extends AbstractTestNGSpringContextTests {

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

    @BeforeClass
    public final void apiServer() {
        this.apiServer = ClientAndServer.startClientAndServer(apiPort);
        this.apiClient = new MockServerClient("localhost", apiPort);
    }

    @BeforeClass
    public final void authorizationServer() {
        this.authorizationServer = ClientAndServer.startClientAndServer(authorizationPort);
        this.authorizationClient = new MockServerClient("localhost", authorizationPort);
    }

    @AfterClass
    public final void closeMockServer() {

        this.apiServer.close();
        this.apiServer.hasStopped();
        this.authorizationServer.close();
        this.authorizationServer.hasStopped();
    }

}
