package com.exemple.gateway.core.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.JsonBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.test.context.ActiveProfiles;

import com.exemple.gateway.core.GatewayServerTestConfiguration;
import com.exemple.gateway.core.common.LoggingFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import io.restassured.RestAssured;
import io.restassured.http.Cookie;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.extern.slf4j.Slf4j;

@ActiveProfiles("browser")
@Slf4j
class SecurityCookieTest extends GatewayServerTestConfiguration {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private SessionRepository<Session> repository;

    private RequestSpecification requestSpecification;

    private SignedJWT ACCESS_TOKEN;
    private SignedJWT REFRESH_TOKEN;

    @Autowired
    private RSASSASigner signer;

    @Autowired
    private Clock clock;

    @BeforeAll
    void init() throws JOSEException {

        var payloadAccessToken = new JWTClaimsSet.Builder()
                .claim("user_name", "john_doe")
                .audience("test")
                .expirationTime(Date.from(Instant.now(clock).plus(1, ChronoUnit.DAYS)))
                .claim("scope", new String[] { "account:read" })
                .build();

        ACCESS_TOKEN = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).build(), payloadAccessToken);
        ACCESS_TOKEN.sign(signer);

        var payloadRefreshToken = new JWTClaimsSet.Builder()
                .claim("user_name", "john_doe")
                .audience("test")
                .claim("scope", new String[] { "account:read" })
                .build();

        REFRESH_TOKEN = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).build(), payloadRefreshToken);
        REFRESH_TOKEN.sign(signer);

    }

    @BeforeEach
    public void before() {

        requestSpecification = RestAssured.given().filters(new LoggingFilter(LOG));

        apiClient.reset();
        authorizationClient.reset();

    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @TestMethodOrder(OrderAnnotation.class)
    class AccessTokenAndRefreshTokenTest {

        private Cookie sessionId;

        private Cookie xsrfToken;

        @Test
        @Order(1)
        void token() throws IOException {

            // Given mock client
            Map<String, Object> responseBody = Map.of(
                    "access_token", ACCESS_TOKEN.serialize(),
                    "refresh_token", REFRESH_TOKEN.serialize(),
                    "scope", "account:read");

            authorizationClient.when(HttpRequest.request().withMethod("POST").withPath("/ExempleAuthorization/oauth/token"))
                    .respond(HttpResponse.response().withBody(MAPPER.writeValueAsString(responseBody)).withStatusCode(200));

            // When perform post
            Response response = requestSpecification.post(restTemplate.getRootUri() + "/ExempleAuthorization/oauth/token");

            // Then check response
            assertAll(
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value()),
                    () -> assertThat(response.getCookies()).isNotEmpty(),
                    () -> assertThat(response.getCookie("JSESSIONID")).isNotNull(),
                    () -> assertThat(response.getCookie("XSRF-TOKEN")).isNotNull(),
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

            // and check session in repository
            assertThat(repository.findById(sessionId.getValue())).isNotNull();

        }

        @Test
        @Order(2)
        void securitySuccess() {

            // Given mock client
            apiClient
                    .when(HttpRequest.request().withMethod("POST").withHeader("Authorization", "BEARER " + ACCESS_TOKEN.serialize())
                            .withPath("/ExempleService/account"))
                    .respond(HttpResponse.response().withHeaders(new Header("Content-Type", "application/json;charset=UTF-8"))
                            .withBody(JsonBody.json(Collections.singletonMap("name", "jean"))).withStatusCode(200));

            // When perform post
            Response response = requestSpecification.cookie("JSESSIONID", sessionId.getValue()).cookie("XSRF-TOKEN", xsrfToken.getValue())
                    .header("X-XSRF-TOKEN", xsrfToken.getValue()).post(restTemplate.getRootUri() + "/ExempleService/account");

            // Then check response
            assertAll(
                    () -> assertThat(response.getStatusCode()).as(response.getBody().prettyPrint()).isEqualTo(HttpStatus.OK.value()),
                    () -> assertThat(response.jsonPath().getString("name")).isEqualTo("jean"));

        }

        @Test
        @Order(2)
        void securityFailureBadCsrfToken() throws IOException {

            // When perform post
            Response response = requestSpecification.cookie("JSESSIONID", sessionId.getValue()).cookie("XSRF-TOKEN", xsrfToken.getValue())
                    .header("X-XSRF-TOKEN", "toto").post(restTemplate.getRootUri() + "/ExempleService/account");

            // Then check response
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());

            // And check error
            assertThat(response.asString()).isEqualTo("Invalid CSRF Token");

        }

        @Test
        @Order(3)
        void replayToken() throws IOException {

            // Given mock client
            Map<String, Object> responseBody = Map.of(
                    "access_token", ACCESS_TOKEN.serialize(),
                    "refresh_token", REFRESH_TOKEN.serialize(),
                    "scope", "account:read");

            authorizationClient.when(HttpRequest.request().withMethod("POST").withPath("/ExempleAuthorization/oauth/token"))
                    .respond(HttpResponse.response().withBody(MAPPER.writeValueAsString(responseBody)).withStatusCode(200));

            // When perform post
            Response response = requestSpecification.cookie("JSESSIONID", sessionId.getValue()).cookie("XSRF-TOKEN", xsrfToken.getValue())
                    .post(restTemplate.getRootUri() + "/ExempleAuthorization/oauth/token");

            // Then check response
            assertAll(
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value()),
                    () -> assertThat(response.getCookies()).isNotEmpty(),
                    () -> assertThat(response.getDetailedCookie("JSESSIONID").getValue()).isNotEqualTo(sessionId.getValue()),
                    () -> assertThat(response.jsonPath().getString("scope")).isEqualTo("account:read"));

            // and check session in repository
            assertThat(repository.findById(sessionId.getValue())).isNull();

        }

    }

    @Test
    void securitySuccessWithSessionIdInHeader() throws IOException {

        // Given build cookie
        Map<String, Object> responseBody = Map.of(
                "access_token", ACCESS_TOKEN.serialize(),
                "refresh_token", REFRESH_TOKEN.serialize(),
                "scope", "account:read");

        authorizationClient.when(HttpRequest.request().withMethod("POST").withPath("/ExempleAuthorization/oauth/token"))
                .respond(HttpResponse.response().withBody(MAPPER.writeValueAsString(responseBody)).withStatusCode(200));

        Response authorizationResponse = requestSpecification.cookie("JSESSIONID", UUID.randomUUID())
                .post(restTemplate.getRootUri() + "/ExempleAuthorization/oauth/token");

        Cookie sessionId = authorizationResponse.getDetailedCookie("JSESSIONID");
        Cookie xsrfToken = authorizationResponse.getDetailedCookie("XSRF-TOKEN");

        // And mock client
        apiClient
                .when(HttpRequest.request().withMethod("POST").withHeader("Authorization", "BEARER " + ACCESS_TOKEN.serialize())
                        .withPath("/ExempleService/account"))
                .respond(HttpResponse.response().withHeaders(new Header("Content-Type", "application/json;charset=UTF-8"))
                        .withBody(JsonBody.json(Collections.singletonMap("name", "jean"))).withStatusCode(200));

        requestSpecification = RestAssured.given().filters(new LoggingFilter(LOG));

        // When perform post
        Response response = requestSpecification.cookie("JSESSIONID", sessionId.getValue()).cookie("XSRF-TOKEN", xsrfToken.getValue())
                .header("X-XSRF-TOKEN", xsrfToken.getValue()).post(restTemplate.getRootUri() + "/ExempleService/account");

        // Then check response
        assertAll(
                () -> assertThat(response.getStatusCode()).as(response.getBody().prettyPrint()).isEqualTo(HttpStatus.OK.value()),
                () -> assertThat(response.jsonPath().getString("name")).isEqualTo("jean"));

    }

    @Test
    void noAccessOrRefreshToken() throws IOException {

        // Given mock client
        Map<String, Object> responseBody = Map.of("scope", "account:read");

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
    void tokenFailure() {

        // Given mock client
        authorizationClient.when(HttpRequest.request().withMethod("POST").withPath("/ExempleAuthorization/oauth/token"))
                .respond(HttpResponse.response().withBody("toto").withStatusCode(200));

        // When perform post
        Response response = requestSpecification.post(restTemplate.getRootUri() + "/ExempleAuthorization/oauth/token");

        // Then check response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());

    }

    @Test
    void tokenIsUnauthorized() {

        // Given mock client
        authorizationClient.when(HttpRequest.request().withMethod("POST").withPath("/ExempleAuthorization/oauth/token"))
                .respond(HttpResponse.response().withStatusCode(HttpStatus.UNAUTHORIZED.value()));

        // When perform post
        Response response = requestSpecification.post(restTemplate.getRootUri() + "/ExempleAuthorization/oauth/token");

        // Then check response
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value()),
                () -> assertThat(response.getCookies()).isNotEmpty(),
                () -> assertThat(response.getCookie("JSESSIONID")).isNull());

    }

}
