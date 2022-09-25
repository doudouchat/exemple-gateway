package com.exemple.gateway.core.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.JsonBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.exemple.gateway.core.GatewayServerTestConfiguration;
import com.exemple.gateway.core.common.LoggingFilter;
import com.exemple.gateway.core.security.token.validator.NotBlackListTokenValidator;
import com.hazelcast.core.HazelcastInstance;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class SecurityAuthorizationHeaderTest extends GatewayServerTestConfiguration {

    @Autowired
    private TestRestTemplate restTemplate;

    private RequestSpecification requestSpecification;

    @Autowired
    private Algorithm algo;

    @Autowired
    private Algorithm otherAlgo;

    @Autowired
    private HazelcastInstance hazelcastInstance;

    @BeforeEach
    private void before() {

        requestSpecification = RestAssured.given().filters(new LoggingFilter(LOG));

        apiClient.reset();

    }

    @Test
    void securitySuccess() {

        // Given build token
        String token = JWT.create().withJWTId(UUID.randomUUID().toString()).withClaim("user_name", "john_doe").withAudience("test")
                .withArrayClaim("scope", new String[] { "account:read" }).sign(algo);

        // And mock client
        apiClient.when(HttpRequest.request().withMethod("POST").withHeader("Authorization", "BEARER " + token).withPath("/ExempleService/account"))
                .respond(HttpResponse.response().withHeaders(new Header("Content-Type", "application/json;charset=UTF-8"))
                        .withBody(JsonBody.json(Collections.singletonMap("name", "jean"))).withStatusCode(200));

        // When perform header
        Response response = requestSpecification.header("Authorization", "BEARER " + token)
                .post(restTemplate.getRootUri() + "/ExempleService/account");

        // Then check response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value());

    }

    @Test
    void securityFailureExpiredTime() {

        // Given build token
        Instant expiredDate = Instant.now().minus(1, ChronoUnit.DAYS);
        String token = JWT.create().withClaim("user_name", "john_doe").withAudience("test").withArrayClaim("scope", new String[] { "account:read" })
                .withExpiresAt(Date.from(expiredDate)).sign(algo);

        // When perform header
        Response response = requestSpecification.header("Authorization", "BEARER " + token)
                .post(restTemplate.getRootUri() + "/ExempleService/account");

        // Then check response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());

        // And check error
        assertThat(response.getHeader("WWW-Authenticate")).contains("error_description=\"Jwt expired");

    }

    @Test
    void securityFailureTokenInBlackList() {

        // Given build token
        String jwtId = UUID.randomUUID().toString();
        String token = JWT.create().withJWTId(jwtId).withClaim("user_name", "john_doe").withAudience("test")
                .withArrayClaim("scope", new String[] { "account:read" }).sign(algo);
        hazelcastInstance.getMap(NotBlackListTokenValidator.TOKEN_BLACK_LIST).put(jwtId, token);

        // When perform header
        Response response = requestSpecification.header("Authorization", "BEARER " + token)
                .post(restTemplate.getRootUri() + "/ExempleService/account");

        // Then check response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());

        // And check body
        assertThat(response.getHeader("WWW-Authenticate")).contains("error_description=\"Jwt is excluded\"");

    }

    @Test
    void securityFailurePublicKeyDoesntCheckSignature() {

        // Given build token
        String token = JWT.create().withJWTId(UUID.randomUUID().toString()).withClaim("user_name", "john_doe").withAudience("test")
                .withArrayClaim("scope", new String[] { "account:read" }).sign(otherAlgo);

        // When perform header
        Response response = requestSpecification.header("Authorization", "BEARER " + token)
                .post(restTemplate.getRootUri() + "/ExempleService/account");

        // Then check response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());

        // And check body
        assertThat(response.getHeader("WWW-Authenticate")).contains("error_description=\"Failed to validate the token\"");

    }

}
