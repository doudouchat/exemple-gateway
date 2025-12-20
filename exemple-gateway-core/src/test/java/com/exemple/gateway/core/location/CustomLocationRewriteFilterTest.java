package com.exemple.gateway.core.location;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.exemple.gateway.core.GatewayServerTestConfiguration;
import com.exemple.gateway.core.common.LoggingFilter;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.MockResponse;

@Slf4j
class CustomLocationRewriteFilterTest extends GatewayServerTestConfiguration {

    private RequestSpecification requestSpecification;

    @BeforeEach
    void before() {

        requestSpecification = RestAssured.given().filters(new LoggingFilter(LOG)).port(this.localPort);

    }

    @Test
    void location201Rewrite() {

        // Given mock server
        apiServer.url("/ExempleService/info");
        apiServer.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("Location", "http://localhost:" + this.apiServer.getPort() + "/ExempleService/123")
                .setResponseCode(201));

        // When perform get
        Response response = requestSpecification.get("/ExempleService/info");

        // Then check response
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED.value()),
                () -> assertThat(response.getHeader("Location")).endsWith("/ExempleService/123"));

    }

    @Test
    void location302Rewrite() {

        // Given mock server
        authorizationServer.url("/ExempleAuthorization/oauth/info");
        authorizationServer.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("Location", "http://localhost:" + this.authorizationServer.getPort() + "/ExempleAuthorization/123")
                .setResponseCode(302));

        // When perform post
        Response response = requestSpecification.post("/ExempleAuthorization/oauth/info");

        // Then check response
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND.value()),
                () -> assertThat(response.getHeader("Location")).endsWith("/ExempleAuthorization/123"));

    }

}
