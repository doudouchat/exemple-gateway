package com.exemple.gateway.integration.cookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.exemple.gateway.integration.common.JsonRestTemplate;

import io.restassured.http.ContentType;
import io.restassured.http.Cookie;
import io.restassured.response.Response;

@TestMethodOrder(OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestCookieIT {

    private static final String URL = "/ws/test";

    private Cookie sessionId;
    private Cookie xsrfToken;

    @Test
    @Order(1)
    void token() {

        // When perform post
        Map<String, String> params = Map.of("grant_type", "client_credentials");

        Response response = JsonRestTemplate.given(JsonRestTemplate.APPLICATION_URL, ContentType.URLENC)
                .header("Authorization", "Basic " + Base64.encodeBase64String("resource:secret".getBytes(StandardCharsets.UTF_8)))
                .formParams(params).post("/oauth/token");

        // Then check response
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(200),
                () -> assertThat(response.getCookies()).isNotEmpty(),
                () -> assertThat(response.getCookie("JSESSIONID")).isNotNull(),
                () -> assertThat(response.getCookie("XSRF-TOKEN")).isNotNull());

        sessionId = response.getDetailedCookie("JSESSIONID");
        xsrfToken = response.getDetailedCookie("XSRF-TOKEN");

        // And check session
        assertThat(sessionId.isHttpOnly()).isTrue();

        // And check token
        assertThat(xsrfToken.isHttpOnly()).isFalse();

    }

    @Test
    @Order(3)
    void retryToken() {

        // When perform post
        Map<String, String> params = Map.of("grant_type", "client_credentials");

        Response response = JsonRestTemplate.given(JsonRestTemplate.APPLICATION_URL, ContentType.URLENC)

                .cookie("JSESSIONID", sessionId.getValue())

                .header("Authorization", "Basic " + Base64.encodeBase64String("resource:secret".getBytes(StandardCharsets.UTF_8)))
                .formParams(params).post("/oauth/token");

        // Then check response
        assertThat(response.getStatusCode()).isEqualTo(200);

    }

    @Test
    @Order(2)
    void post() {

        // When perform post
        Map<String, Object> body = Map.of("value", UUID.randomUUID());

        Response response = JsonRestTemplate.given()

                .cookie("JSESSIONID", sessionId.getValue()).cookie("XSRF-TOKEN", xsrfToken.getValue()).queryParam("debug", "true")

                .header("X-XSRF-TOKEN", xsrfToken.getValue())

                .body(body).post(URL);

        // Then check response
        assertThat(response.getStatusCode()).isEqualTo(201);

    }

    private Stream<Arguments> postFailure() {

        return Stream.of(
                Arguments.of("bad jsessionId", xsrfToken.getValue(), 401),
                Arguments.of(sessionId.getValue(), "bad xrsf token", 403));
    }

    @ParameterizedTest
    @MethodSource
    @Order(2)
    void postFailure(String jsessionId, String xrsfToken, int expectedStatus) {

        // When perform post
        Map<String, Object> body = Map.of("value", UUID.randomUUID());

        Response response = JsonRestTemplate.given()

                .cookie("JSESSIONID", jsessionId).cookie("XSRF-TOKEN", xrsfToken).queryParam("debug", "true")

                .header("X-XSRF-TOKEN", xsrfToken.getValue())

                .body(body).post(URL);

        // Then check response
        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);

    }

    @Test
    @Order(2)
    void head() {

        // When perform head
        Response response = JsonRestTemplate.given()

                .cookie("JSESSIONID", sessionId.getValue()).cookie("XSRF-TOKEN", xsrfToken.getValue()).queryParam("debug", "true")

                .header("X-XSRF-TOKEN", xsrfToken.getValue())

                .head(URL + "/{id}", "123");

        // Then check response
        assertThat(response.getStatusCode()).isEqualTo(204);

    }

    @Test
    @Order(2)
    void get() {

        // When perform get
        Response response = JsonRestTemplate.given()

                .cookie("JSESSIONID", sessionId.getValue()).cookie("XSRF-TOKEN", xsrfToken.getValue()).queryParam("debug", "true")

                .get(URL + "/{id}", "123");

        // Then check response
        assertThat(response.getStatusCode()).isEqualTo(200);

    }

    @Test
    @Order(2)
    void delete() {

        // When perform delete
        Response response = JsonRestTemplate.given()

                .cookie("JSESSIONID", sessionId.getValue()).cookie("XSRF-TOKEN", xsrfToken.getValue()).queryParam("debug", "true")

                .header("X-XSRF-TOKEN", xsrfToken.getValue())

                .delete(URL + "/{id}", "123");

        // Then check response
        assertThat(response.getStatusCode()).isEqualTo(204);

    }

    @Test
    @Order(2)
    void patch() {

        // When perform patch
        Map<String, Object> patch = Map.of(
                "op", "replace",
                "path", "/value",
                "value", UUID.randomUUID());

        Response response = JsonRestTemplate.given()

                .cookie("JSESSIONID", sessionId.getValue()).cookie("XSRF-TOKEN", xsrfToken.getValue()).queryParam("debug", "true")

                .header("X-XSRF-TOKEN", xsrfToken.getValue())

                .body(Collections.singletonList(patch)).patch(URL + "/{id}", "123");

        // Then check response
        assertThat(response.getStatusCode()).isEqualTo(204);

    }

    @Test
    @Order(2)
    void options() {

        // When perform options
        Response response = JsonRestTemplate.given()

                .cookie("JSESSIONID", sessionId.getValue()).cookie("XSRF-TOKEN", xsrfToken.getValue()).queryParam("debug", "true")

                .options(URL + "/{id}", "123");

        // Then check response
        assertThat(response.getStatusCode()).isEqualTo(200);

    }
}
