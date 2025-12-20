package com.exemple.gateway.core.redirection;

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
class RedirectionFilterTest extends GatewayServerTestConfiguration {

    private RequestSpecification requestSpecification;

    @BeforeEach
    void before() {

        requestSpecification = RestAssured.given().filters(new LoggingFilter(LOG)).port(this.localPort);

    }

    @Test
    void redirectionMoveToOk() {

        // Given mock server
        authorizationServer.url("/ExempleAuthorization/login");
        authorizationServer.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json;charset=UTF-8")
                .addHeader("Location", "http://localhost:" + this.authorizationServer.getPort() + "/ExempleAuthorization/123")
                .setResponseCode(302));

        // When perform post
        Response response = requestSpecification.post("/ExempleAuthorization/login");

        // Then check response
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value()),
                () -> assertThat(response.getHeader("Location")).isNull());

    }

    @Test
    void redirectionMoveToKO() {

        // Given mock server
        authorizationServer.url("/ExempleAuthorization/login");
        authorizationServer.enqueue(new MockResponse()
                .setResponseCode(401));

        // When perform post
        Response response = requestSpecification.post("/ExempleAuthorization/login");

        // Then check response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());

    }

}
