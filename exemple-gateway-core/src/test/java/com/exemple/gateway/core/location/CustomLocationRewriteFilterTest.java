package com.exemple.gateway.core.location;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.http.HttpStatus;

import com.exemple.gateway.core.GatewayServerTestConfiguration;
import com.exemple.gateway.core.common.LoggingFilter;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class CustomLocationRewriteFilterTest extends GatewayServerTestConfiguration {

    private RequestSpecification requestSpecification;

    @BeforeEach
    void before() {

        requestSpecification = RestAssured.given().filters(new LoggingFilter(LOG)).port(this.localPort);

        apiClient.reset();
        authorizationClient.reset();

    }

    @Test
    void location201Rewrite() {

        // Given mock client
        apiClient.when(HttpRequest.request().withMethod("GET").withPath("/ExempleService/info"))
                .respond(
                        HttpResponse.response()
                                .withHeaders(new Header("Content-Type", "application/json;charset=UTF-8"),
                                        new Header("Location", "http://localhost:" + this.apiClient.getPort() + "/ExempleService/123"))
                                .withStatusCode(201));

        // When perform get
        Response response = requestSpecification.get("/ExempleService/info");

        // Then check response
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED.value()),
                () -> assertThat(response.getHeader("Location")).endsWith("/ExempleService/123"));

    }

    @Test
    void location302Rewrite() {

        // Given mock client
        authorizationClient.when(HttpRequest.request().withMethod("POST").withPath("/ExempleAuthorization/oauth/info"))
                .respond(HttpResponse.response()
                        .withHeaders(new Header("Content-Type", "application/json;charset=UTF-8"),
                                new Header("Location", "http://localhost:" + this.authorizationClient.getPort() + "/ExempleAuthorization/123"))
                        .withStatusCode(302));

        // When perform post
        Response response = requestSpecification.post("/ExempleAuthorization/oauth/info");

        // Then check response
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND.value()),
                () -> assertThat(response.getHeader("Location")).endsWith("/ExempleAuthorization/123"));

    }

}
