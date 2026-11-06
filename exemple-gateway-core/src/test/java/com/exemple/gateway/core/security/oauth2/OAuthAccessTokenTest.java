package com.exemple.gateway.core.security.oauth2;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.session.SessionRepository;
import org.springframework.test.context.ActiveProfiles;

import com.exemple.gateway.core.GatewayServerTestConfiguration;
import com.exemple.gateway.core.common.LoggingFilter;
import com.hazelcast.spring.session.HazelcastIndexedSessionRepository.HazelcastSession;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.MockResponse;
import tools.jackson.databind.ObjectMapper;

@ActiveProfiles("browser")
@Slf4j
class OAuthAccessTokenTest extends GatewayServerTestConfiguration {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private SessionRepository<HazelcastSession> repository;

    private RequestSpecification requestSpecification;

    private SignedJWT accessToken;
    private SignedJWT refreshToken;

    @Autowired
    private RSASSASigner signer;

    @Autowired
    private Clock clock;

    @BeforeAll
    void init() throws JOSEException {

        var payloadAccessToken = new JWTClaimsSet.Builder()
                .claim("user_name", "john_doe")
                .audience("test")
                .subject("john_doe")
                .expirationTime(Date.from(Instant.now(clock).plus(1, ChronoUnit.DAYS)))
                .claim("scope", new String[] { "account:read" })
                .build();

        accessToken = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).build(), payloadAccessToken);
        accessToken.sign(signer);

        var payloadRefreshToken = new JWTClaimsSet.Builder()
                .claim("user_name", "john_doe")
                .audience("test")
                .claim("scope", new String[] { "account:read" })
                .build();

        refreshToken = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).build(), payloadRefreshToken);
        refreshToken.sign(signer);

    }

    @BeforeEach
    void before() {

        requestSpecification = RestAssured.given().filters(new LoggingFilter(LOG)).port(this.localPort);

    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class AnonymousTest {

        private String sessionId;

        @Test
        @Order(1)
        void securityAnonymousSuccess() throws InterruptedException {

            // Given mock server
            apiServer.enqueue(new MockResponse()
                    .setResponseCode(200));

            // And mock authorization
            var responseBody = Map.of(
                    "access_token", accessToken.serialize(),
                    "scope", "account:read",
                    "token_type", "Bearer",
                    "expires_in", 300);
            authorizationServer.enqueue(new MockResponse()
                    .addHeader("Content-Type", "application/json;charset=UTF-8")
                    .setBody(MAPPER.writeValueAsString(responseBody))
                    .setResponseCode(200));

            // When perform post
            Response response = requestSpecification
                    .cookie("XSRF-TOKEN", "test")
                    .header("X-XSRF-TOKEN", "test")
                    .get("/ExempleService");

            // Then check response
            assertThat(response.getStatusCode()).as(response.getBody().prettyPrint()).isEqualTo(HttpStatus.OK.value());

            // And check request
            assertThat(apiServer.takeRequest().getHeader("Authorization")).isEqualTo("Bearer " + accessToken.serialize());

            // And check session
            sessionId = response.cookie("JSESSIONID");
            HazelcastSession session = repository.findById(sessionId);
            assertThat(session).isNotNull();
            Map<String, Object> attributes = session.getAttribute(
                    "org.springframework.security.oauth2.client.web.server.WebSessionServerOAuth2AuthorizedClientRepository.AUTHORIZED_CLIENTS");
            OAuth2AuthorizedClient authorizedClient = (OAuth2AuthorizedClient) attributes.get("exemple-app");
            assertThat(authorizedClient.getAccessToken().getTokenValue()).isEqualTo(accessToken.serialize());
            assertThat(authorizedClient.getPrincipalName()).isEqualTo("app");

        }

        @Test
        @Order(2)
        void securityAnonymousSuccessWithSessionId() throws InterruptedException {

            // Given mock server
            apiServer.enqueue(new MockResponse()
                    .setResponseCode(200));

            // When perform post
            Response response = requestSpecification
                    .cookie("JSESSIONID", sessionId)
                    .cookie("XSRF-TOKEN", "test")
                    .header("X-XSRF-TOKEN", "test")
                    .get("/ExempleService");

            // Then check response
            assertThat(response.getStatusCode()).as(response.getBody().prettyPrint()).isEqualTo(HttpStatus.OK.value());

            // And check request
            assertThat(apiServer.takeRequest().getHeader("Authorization")).isEqualTo("Bearer " + accessToken.serialize());

            // And check session
            HazelcastSession session = repository.findById(sessionId);
            assertThat(session).isNotNull();
            Map<String, Object> attributes = session.getAttribute(
                    "org.springframework.security.oauth2.client.web.server.WebSessionServerOAuth2AuthorizedClientRepository.AUTHORIZED_CLIENTS");
            OAuth2AuthorizedClient authorizedClient = (OAuth2AuthorizedClient) attributes.get("exemple-app");
            assertThat(authorizedClient.getAccessToken().getTokenValue()).isEqualTo(accessToken.serialize());
            assertThat(authorizedClient.getPrincipalName()).isEqualTo("app");

        }

    }
}
