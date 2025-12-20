package com.exemple.gateway.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.exemple.gateway.core.common.LoggingFilter;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.MockResponse;
import tools.jackson.databind.ObjectMapper;

@Slf4j
class RoutesTest extends GatewayServerTestConfiguration {

    private RequestSpecification requestSpecification;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeEach
    void before() {

        requestSpecification = RestAssured.given().filters(new LoggingFilter(LOG)).port(this.localPort);

    }

    @Test
    void api() {

        // Given mock server
        apiServer.url("/ExempleService/info");
        apiServer.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("Location", "http://localhost:" + this.apiServer.getPort() + "/ExempleService/123")
                .setResponseCode(200)
                .setBody(MAPPER.writeValueAsString(Collections.singletonMap("name", "jean"))));

        // When perform get
        Response response = requestSpecification.get("/ExempleService/info");

        // Then check response
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value()),
                () -> assertThat(response.jsonPath().getString("name")).isEqualTo("jean"));

    }

    @Test
    void authorization() {

        // Given mock server
        authorizationServer.url("/ExempleAuthorization/info");
        authorizationServer.enqueue(new MockResponse()
                .setResponseCode(200));

        // When perform get
        Response response = requestSpecification.get("/ExempleAuthorization/info");

        // Then check response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value());

    }

}
