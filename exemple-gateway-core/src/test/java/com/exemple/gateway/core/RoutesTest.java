package com.exemple.gateway.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.JsonBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;

import com.exemple.gateway.core.common.LoggingFilter;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class RoutesTest extends GatewayServerTestConfiguration {

    @Autowired
    private TestRestTemplate restTemplate;

    private RequestSpecification requestSpecification;

    @BeforeEach
    public void before() {

        requestSpecification = RestAssured.given().filters(new LoggingFilter(LOG));

        apiClient.reset();
        authorizationClient.reset();

    }

    @Test
    void api() {

        // Given mock client
        apiClient.when(HttpRequest.request().withMethod("GET").withPath("/ExempleService/info"))
                .respond(HttpResponse.response().withHeaders(new Header("Content-Type", "application/json;charset=UTF-8"))
                        .withBody(JsonBody.json(Collections.singletonMap("name", "jean"))).withStatusCode(200));

        // When perform get
        Response response = requestSpecification.get(restTemplate.getRootUri() + "/ExempleService/info");

        // Then check response
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value()),
                () -> assertThat(response.jsonPath().getString("name")).isEqualTo("jean"));

    }

    @Test
    void authorization() {

        // Given mock client
        authorizationClient.when(HttpRequest.request().withMethod("GET").withPath("/ExempleAuthorization/info"))
                .respond(HttpResponse.response().withStatusCode(200));

        // When perform get
        Response response = requestSpecification.get(restTemplate.getRootUri() + "/ExempleAuthorization/info");

        // Then check response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value());

    }

}
