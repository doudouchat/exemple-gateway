package com.exemple.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.JsonBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.exemple.gateway.common.LoggingFilter;
import com.exemple.gateway.core.GatewayServerTestConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.restassured.RestAssured;
import io.restassured.http.Cookie;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.extern.slf4j.Slf4j;

@ActiveProfiles("browser")
@TestMethodOrder(OrderAnnotation.class)
@Slf4j
public class SecurityCookieTest extends GatewayServerTestConfiguration {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private TestRestTemplate restTemplate;

    private RequestSpecification requestSpecification;

    @Autowired
    private Algorithm algo;

    private Cookie sessionId;
    private Cookie xsrfToken;

    private String ACCESS_TOKEN;
    private String DEPRECATED_ACCESS_TOKEN;

    private String REFRESH_TOKEN;

    @Autowired
    private Clock clock;

    @BeforeAll
    public void init() {

        ACCESS_TOKEN = JWT.create().withClaim("user_name", "john_doe").withAudience("test")
                .withExpiresAt(Date.from(Instant.now(clock).plus(1, ChronoUnit.DAYS))).withArrayClaim("scope", new String[] { "account:read" })
                .sign(algo);

        DEPRECATED_ACCESS_TOKEN = JWT.create().withClaim("user_name", "john_doe").withAudience("test")
                .withExpiresAt(Date.from(Instant.now(clock).minus(1, ChronoUnit.DAYS))).withArrayClaim("scope", new String[] { "account:read" })
                .sign(algo);

        REFRESH_TOKEN = JWT.create().withClaim("user_name", "john_doe").withAudience("test").withArrayClaim("scope", new String[] { "account:read" })
                .sign(algo);

    }

    @BeforeEach
    private void before() {

        requestSpecification = RestAssured.given().filters(new LoggingFilter(LOG));

        apiClient.reset();
        authorizationClient.reset();

    }

    @Test
    @Order(1)
    public void token() throws IOException {

        // Given mock client
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("access_token", ACCESS_TOKEN);
        responseBody.put("refresh_token", REFRESH_TOKEN);
        responseBody.put("scope", "account:read");

        authorizationClient.when(HttpRequest.request().withMethod("POST").withPath("/ExempleAuthorization/oauth/token"))
                .respond(HttpResponse.response().withBody(MAPPER.writeValueAsString(responseBody)).withStatusCode(200));

        // When perform post
        Response response = requestSpecification.post(restTemplate.getRootUri() + "/ExempleAuthorization/oauth/token");

        // Then check response
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value()),
                () -> assertThat(response.getCookies()).isNotEmpty(),
                () -> assertThat(response.getCookie("JSESSIONID")).isNotNull(),
                () -> assertThat(response.jsonPath().getString("scope")).isEqualTo("account:read"));

        sessionId = response.getDetailedCookie("JSESSIONID");
        xsrfToken = response.getDetailedCookie("XSRF-TOKEN");

        // And check session
        assertAll(
                () -> assertThat(sessionId.isHttpOnly()).isTrue(),
                () -> assertThat(sessionId.getExpiryDate()).isNull(),
                () -> assertThat(sessionId.getMaxAge()).isEqualTo(-1));

        // And check token
        assertThat(xsrfToken.isHttpOnly()).isFalse();

    }

    @Test
    @Order(2)
    public void securitySuccess() {

        // Given mock client
        apiClient
                .when(HttpRequest.request().withMethod("POST").withHeader("Authorization", "BEARER " + ACCESS_TOKEN)
                        .withPath("/ExempleService/account"))
                .respond(HttpResponse.response().withHeaders(new Header("Content-Type", "application/json;charset=UTF-8"))
                        .withBody(JsonBody.json(Collections.singletonMap("name", "jean"))).withStatusCode(200));

        // When perform post
        Response response = requestSpecification.cookie("JSESSIONID", sessionId.getValue()).cookie("XSRF-TOKEN", xsrfToken.getValue())
                .header("X-XSRF-TOKEN", xsrfToken.getValue()).post(restTemplate.getRootUri() + "/ExempleService/account");

        // Then check response
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value()),
                () -> assertThat(response.jsonPath().getString("name")).isEqualTo("jean"));

    }

    @Test
    public void securitySuccessWithSessionIdInHeader() throws IOException {

        // Given build cookie
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("access_token", ACCESS_TOKEN);
        responseBody.put("refresh_token", REFRESH_TOKEN);
        responseBody.put("scope", "account:read");

        authorizationClient.when(HttpRequest.request().withMethod("POST").withPath("/ExempleAuthorization/oauth/token"))
                .respond(HttpResponse.response().withBody(MAPPER.writeValueAsString(responseBody)).withStatusCode(200));

        Response authorizationResponse = requestSpecification.cookie("JSESSIONID", UUID.randomUUID())
                .post(restTemplate.getRootUri() + "/ExempleAuthorization/oauth/token");

        Cookie sessionId = authorizationResponse.getDetailedCookie("JSESSIONID");
        Cookie xsrfToken = authorizationResponse.getDetailedCookie("XSRF-TOKEN");

        // And mock client
        apiClient
                .when(HttpRequest.request().withMethod("POST").withHeader("Authorization", "BEARER " + ACCESS_TOKEN)
                        .withPath("/ExempleService/account"))
                .respond(HttpResponse.response().withHeaders(new Header("Content-Type", "application/json;charset=UTF-8"))
                        .withBody(JsonBody.json(Collections.singletonMap("name", "jean"))).withStatusCode(200));

        requestSpecification = RestAssured.given().filters(new LoggingFilter(LOG));

        // When perform post
        Response response = requestSpecification.cookie("JSESSIONID", sessionId.getValue()).cookie("XSRF-TOKEN", xsrfToken.getValue())
                .header("X-XSRF-TOKEN", xsrfToken.getValue()).post(restTemplate.getRootUri() + "/ExempleService/account");

        // Then check response
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value()),
                () -> assertThat(response.jsonPath().getString("name")).isEqualTo("jean"));

    }

    private Stream<Arguments> securityFailures() {

        return Stream.of(
                Arguments.of(DEPRECATED_ACCESS_TOKEN, xsrfToken.getValue(), HttpStatus.UNAUTHORIZED),
                Arguments.of(ACCESS_TOKEN, "toto", HttpStatus.FORBIDDEN));
    }

    @ParameterizedTest
    @MethodSource
    @Order(2)
    public void securityFailures(String accessToken, String csrfToken, HttpStatus expectedHttpStatus) throws IOException {

        // Given build cookie
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("access_token", accessToken);
        responseBody.put("refresh_token", REFRESH_TOKEN);
        responseBody.put("scope", "account:read");

        authorizationClient.when(HttpRequest.request().withMethod("POST").withPath("/ExempleAuthorization/oauth/token"))
                .respond(HttpResponse.response().withBody(MAPPER.writeValueAsString(responseBody)).withStatusCode(HttpStatus.OK.value()));

        Response authorizationResponse = requestSpecification.post(restTemplate.getRootUri() + "/ExempleAuthorization/oauth/token");

        Cookie sessionId = authorizationResponse.getDetailedCookie("JSESSIONID");

        // When perform post
        Response response = requestSpecification.cookie("JSESSIONID", sessionId.getValue()).cookie("XSRF-TOKEN", xsrfToken.getValue())
                .header("X-XSRF-TOKEN", csrfToken).post(restTemplate.getRootUri() + "/ExempleService/account");

        // Then check response
        assertThat(response.getStatusCode()).isEqualTo(expectedHttpStatus.value());

    }

    @Test
    public void cookieFailure() throws IOException {

        // Given build cookie
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("access_token", ACCESS_TOKEN);

        authorizationClient.when(HttpRequest.request().withMethod("POST").withPath("/ExempleAuthorization/oauth/token"))
                .respond(HttpResponse.response().withBody(MAPPER.writeValueAsString(responseBody)).withStatusCode(HttpStatus.UNAUTHORIZED.value()));

        // When perform post
        Response authorizationResponse = requestSpecification.post(restTemplate.getRootUri() + "/ExempleAuthorization/oauth/token");

        Cookie sessionId = authorizationResponse.getDetailedCookie("JSESSIONID");

        // Then check cookie
        assertThat(sessionId).isNull();
        ;

    }

    @Test
    public void tokenEmpty() throws IOException {

        // Given mock client
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("scope", "account:read");

        authorizationClient.when(HttpRequest.request().withMethod("POST").withPath("/ExempleAuthorization/oauth/token"))
                .respond(HttpResponse.response().withBody(MAPPER.writeValueAsString(responseBody)).withStatusCode(200));

        // When perform post
        Response response = requestSpecification.post(restTemplate.getRootUri() + "/ExempleAuthorization/oauth/token");

        // Then check response
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value()),
                () -> assertThat(response.getCookies()).isNotEmpty(),
                () -> assertThat(response.getCookie("JSESSIONID")).isNotNull(),
                () -> assertThat(response.jsonPath().getString("scope")).isEqualTo("account:read"));

    }

    @Test
    public void tokenFailure() {

        // Given mock client
        authorizationClient.when(HttpRequest.request().withMethod("POST").withPath("/ExempleAuthorization/oauth/token"))
                .respond(HttpResponse.response().withBody("toto").withStatusCode(200));

        // When perform post
        Response response = requestSpecification.post(restTemplate.getRootUri() + "/ExempleAuthorization/oauth/token");

        // Then check response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());

    }

    @Test
    public void tokenUnauthorized() {

        // Given mock client
        authorizationClient.when(HttpRequest.request().withMethod("POST").withPath("/ExempleAuthorization/oauth/token"))
                .respond(HttpResponse.response().withStatusCode(401));

        // When perform post
        Response response = requestSpecification.post(restTemplate.getRootUri() + "/ExempleAuthorization/oauth/token");

        // Then check response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());

    }

}
