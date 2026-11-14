package com.exemple.gateway.core.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
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

    @Autowired
    private RSASSASigner signer;

    @Autowired
    private Clock clock;

    private RequestSpecification requestSpecification;

    @BeforeEach
    void before() {

        requestSpecification = RestAssured.given().redirects().follow(false).filters(new LoggingFilter(LOG)).port(localPort);

    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ClientCredentialsTest {

        private String sessionId;

        private SignedJWT accessToken;

        @Test
        @Order(1)
        void accessWithoutAuthenticatedApp() throws InterruptedException, JOSEException {

            // Given mock server
            apiServer.enqueue(new MockResponse().setResponseCode(200));

            // And mock authorization
            accessToken = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).build(), new JWTClaimsSet.Builder()
                    .audience("test")
                    .expirationTime(Date.from(Instant.now(clock).plus(1, ChronoUnit.DAYS)))
                    .claim("scope", new String[] { "account:create" })
                    .build());
            accessToken.sign(signer);

            var responseBody = Map.of(
                    "access_token", accessToken.serialize(),
                    "scope", "account:create",
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
                    .post("/Exemple/resource");

            // Then check response
            assertThat(response.getStatusCode()).as(response.getBody().prettyPrint()).isEqualTo(HttpStatus.OK.value());
            assertThat(response.cookie("JSESSIONID")).as("JSESSIONID must not be missing").isNotEmpty();

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
        void accessWithAuthenticatedApp() throws InterruptedException {

            // Given mock server
            apiServer.enqueue(new MockResponse().setResponseCode(200));

            // When perform post
            Response response = requestSpecification
                    .cookie("JSESSIONID", sessionId)
                    .cookie("XSRF-TOKEN", "test")
                    .header("X-XSRF-TOKEN", "test")
                    .post("/Exemple/resource");

            // Then check response
            assertThat(response.getStatusCode()).as(response.getBody().prettyPrint()).isEqualTo(HttpStatus.OK.value());
            assertThat(response.cookie("JSESSIONID")).as("JSESSIONID must be missing").isNullOrEmpty();

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

        @Test
        void accessWithAuthenticatedAppAndDeprecatedToken() throws InterruptedException, JOSEException {

            // Given mock server
            apiServer.enqueue(new MockResponse().setResponseCode(200));

            // create deprecated access token
            var deprecatedAccessToken = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).build(), new JWTClaimsSet.Builder()
                    .audience("test")
                    .expirationTime(Date.from(Instant.now(clock).minus(1, ChronoUnit.DAYS)))
                    .claim("scope", new String[] { "account:create" })
                    .build());
            deprecatedAccessToken.sign(signer);

            // And create session with deprecated token
            HazelcastSession session = repository.createSession();

            OAuth2AuthorizedClient authorizedClient = new OAuth2AuthorizedClient(
                    ClientRegistration
                            .withRegistrationId("exemple-app")
                            .clientId("client-app")
                            .tokenUri("http://localhost:" + authorizationServer.getPort() + "/oauth/token")
                            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS).build(),
                    "app",
                    new OAuth2AccessToken(
                            OAuth2AccessToken.TokenType.BEARER,
                            deprecatedAccessToken.serialize(),
                            Instant.now(clock).minus(2, ChronoUnit.DAYS),
                            Instant.now(clock).minus(1, ChronoUnit.DAYS)));
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("exemple-app", authorizedClient);
            session.setAttribute(
                    "org.springframework.security.oauth2.client.web.server.WebSessionServerOAuth2AuthorizedClientRepository.AUTHORIZED_CLIENTS",
                    attributes);

            repository.save(session);

            // And mock authorization
            var newAccessToken = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).build(), new JWTClaimsSet.Builder()
                    .audience("test")
                    .expirationTime(Date.from(Instant.now(clock).plus(1, ChronoUnit.DAYS)))
                    .claim("scope", new String[] { "account:create" })
                    .build());
            newAccessToken.sign(signer);

            var responseBody = Map.of(
                    "access_token", newAccessToken.serialize(),
                    "scope", "account:create",
                    "token_type", "Bearer",
                    "expires_in", 300);
            authorizationServer.enqueue(new MockResponse()
                    .addHeader("Content-Type", "application/json;charset=UTF-8")
                    .setBody(MAPPER.writeValueAsString(responseBody))
                    .setResponseCode(200));

            // When perform post
            Response response = requestSpecification
                    .cookie("JSESSIONID", session.getId())
                    .cookie("XSRF-TOKEN", "test")
                    .header("X-XSRF-TOKEN", "test")
                    .post("/Exemple/resource");

            // Then check response
            assertThat(response.getStatusCode()).as(response.getBody().prettyPrint()).isEqualTo(HttpStatus.OK.value());
            assertThat(response.cookie("JSESSIONID")).as("JSESSIONID must be missing").isNullOrEmpty();

            // And check request
            assertThat(apiServer.takeRequest().getHeader("Authorization")).isEqualTo("Bearer " + newAccessToken.serialize());

            // And check session
            HazelcastSession actualSession = repository.findById(session.getId());
            assertThat(actualSession).isNotNull();
            Map<String, Object> newAttributes = actualSession.getAttribute(
                    "org.springframework.security.oauth2.client.web.server.WebSessionServerOAuth2AuthorizedClientRepository.AUTHORIZED_CLIENTS");
            OAuth2AuthorizedClient newAuthorizedClient = (OAuth2AuthorizedClient) newAttributes.get("exemple-app");
            assertThat(newAuthorizedClient.getAccessToken().getTokenValue()).isEqualTo(newAccessToken.serialize());
            assertThat(newAuthorizedClient.getPrincipalName()).isEqualTo("app");

        }

    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class AuthorizationCodeTest {

        private String sessionId;

        private String location;

        private SignedJWT accessToken;

        private OAuth2AuthorizationRequest authorizationRequest;

        @Test
        @Order(1)
        void accessWithoutAuthenticatedUser() {

            // When perform post
            Response response = requestSpecification
                    .cookie("XSRF-TOKEN", "test")
                    .header("X-XSRF-TOKEN", "test")
                    .get("/Exemple/resource/123");

            // Then check response
            assertThat(response.getStatusCode()).as(response.getBody().prettyPrint()).isEqualTo(HttpStatus.FOUND.value());
            assertThat(response.cookie("JSESSIONID")).as("JSESSIONID must not be missing").isNotEmpty();
            assertThat(response.getHeader("Location")).as("Location header must be present").isNotEmpty();

            sessionId = response.cookie("JSESSIONID");
            location = response.getHeader("Location");

            // And check session
            HazelcastSession session = repository.findById(sessionId);
            assertThat(session).isNotNull();
            authorizationRequest = session.getAttribute(
                    "org.springframework.security.oauth2.client.web.server.WebSessionOAuth2ServerAuthorizationRequestRepository.AUTHORIZATION_REQUEST");
            assertThat(authorizationRequest).usingRecursiveComparison().ignoringFields("attributes", "additionalParameters", "state").isEqualTo(
                    OAuth2AuthorizationRequest.authorizationCode()
                            .authorizationRequestUri(location)
                            .authorizationUri("/ExempleAuthorization/authorize")
                            .clientId("client-user")
                            .redirectUri("http://localhost:%s/login/oauth2/code/exemple-user".formatted(localPort))
                            .build());
        }

        @Test
        @Order(2)
        void redirectToAutorization() {

            // Setup mock authorization
            authorizationServer.enqueue(new MockResponse()
                    .addHeader("Location",
                            "http://localhost:%s/login/oauth2/code/exemple-user?code=%s&state=%s".formatted(authorizationServer.getPort(), "123",
                                    authorizationRequest.getState()))
                    .setResponseCode(302));

            // When perform post
            Response response = requestSpecification
                    .cookie("JSESSIONID", sessionId)
                    .cookie("XSRF-TOKEN", "test")
                    .header("X-XSRF-TOKEN", "test")
                    .get(location);

            // Then check response
            assertThat(response.getStatusCode()).as(response.getBody().prettyPrint()).isEqualTo(HttpStatus.OK.value());
            assertThat(response.cookie("JSESSIONID")).as("JSESSIONID must be missing").isNullOrEmpty();
            assertThat(response.getHeader("Location"))
                    .isEqualTo("http://localhost:%s/login/oauth2/code/exemple-user?code=%s&state=%s".formatted(localPort, "123",
                            authorizationRequest.getState()));

            location = response.getHeader("Location");

        }

        @Test
        @Order(3)
        void redirectToLogin() throws JOSEException {

            // Setup mock authorization
            accessToken = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).build(), new JWTClaimsSet.Builder()
                    .audience("test")
                    .subject("john doe")
                    .expirationTime(Date.from(Instant.now(clock).plus(1, ChronoUnit.DAYS)))
                    .claim("scope", new String[] { "account:create" })
                    .build());
            accessToken.sign(signer);

            var responseBody = Map.of(
                    "access_token", accessToken.serialize(),
                    "scope", "account:create",
                    "token_type", "Bearer",
                    "expires_in", 300);
            authorizationServer.enqueue(new MockResponse()
                    .addHeader("Content-Type", "application/json;charset=UTF-8")
                    .setBody(MAPPER.writeValueAsString(responseBody))
                    .setResponseCode(200));

            // When perform post
            Response response = requestSpecification
                    .cookie("JSESSIONID", sessionId)
                    .cookie("XSRF-TOKEN", "test")
                    .header("X-XSRF-TOKEN", "test")
                    .get(location);

            // Then check response
            assertThat(response.getStatusCode()).as(response.getBody().prettyPrint()).isEqualTo(HttpStatus.FOUND.value());

            // And check session
            HazelcastSession session = repository.findById(sessionId);
            assertThat(session).isNotNull();
            Map<String, Object> attributes = session.getAttribute(
                    "org.springframework.security.oauth2.client.web.server.WebSessionServerOAuth2AuthorizedClientRepository.AUTHORIZED_CLIENTS");
            OAuth2AuthorizedClient authorizedClient = (OAuth2AuthorizedClient) attributes.get("exemple-user");
            assertThat(authorizedClient.getAccessToken().getTokenValue()).isEqualTo(accessToken.serialize());
            assertThat(authorizedClient.getPrincipalName()).isEqualTo("john doe");

        }

        @Test
        @Order(4)
        void accessWithAuthenticatedUser() throws InterruptedException {

            // Given mock server
            apiServer.enqueue(new MockResponse().setResponseCode(200));

            // When perform post
            Response response = requestSpecification
                    .cookie("JSESSIONID", sessionId)
                    .cookie("XSRF-TOKEN", "test")
                    .header("X-XSRF-TOKEN", "test")
                    .get("/Exemple/resource/123");

            // Then check response
            assertThat(response.getStatusCode()).as(response.getBody().prettyPrint()).isEqualTo(HttpStatus.OK.value());
            assertThat(response.cookie("JSESSIONID")).as("JSESSIONID must be missing").isNullOrEmpty();

            // And check request
            assertThat(apiServer.takeRequest().getHeader("Authorization")).isEqualTo("Bearer " + accessToken.serialize());

        }
    }
}
