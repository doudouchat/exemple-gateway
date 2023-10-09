package com.exemple.gateway.core.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.Parameter;
import org.mockserver.model.ParameterBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.test.context.ActiveProfiles;

import com.exemple.gateway.core.GatewayServerTestConfiguration;
import com.exemple.gateway.core.common.LoggingFilter;
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

@ActiveProfiles("browser")
@Slf4j
class OAuthRevokeTokenTest extends GatewayServerTestConfiguration {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private SessionRepository<Session> repository;

    private RequestSpecification requestSpecification;

    private SignedJWT ACCESS_TOKEN;
    private SignedJWT REFRESH_TOKEN;

    @Autowired
    private RSASSASigner signer;

    @BeforeAll
    void init() throws JOSEException {

        var payloadAccessToken = new JWTClaimsSet.Builder()
                .claim("user_name", "john_doe")
                .audience("test")
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
        authorizationClient.reset();

    }

    @Test
    void revokeTokenSuccess() throws IOException {

        // Create session
        var session = repository.createSession();
        session.setAttribute("access_token", ACCESS_TOKEN.serialize());
        session.setAttribute("refresh_token", REFRESH_TOKEN.serialize());
        repository.save(session);

        // And mock client
        authorizationClient.when(HttpRequest.request().withMethod("POST")
                .withBody(new ParameterBody(new Parameter("token", ACCESS_TOKEN.serialize())))
                .withPath("/ExempleAuthorization/oauth/revoke_token"))
                .respond(HttpResponse.response().withStatusCode(204));

        // When perform post
        Response response = requestSpecification
                .cookie("JSESSIONID", session.getId())
                .post(restTemplate.getRootUri() + "/ExempleAuthorization/oauth/revoke_token");

        // Then check response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());

        // And check session
        assertThat(repository.findById(session.getId())).isNull();

    }

    @Test
    void revokeTokenFailure() throws IOException {

        // Create session
        var session = repository.createSession();
        session.setAttribute("access_token", ACCESS_TOKEN.serialize());
        session.setAttribute("refresh_token", REFRESH_TOKEN.serialize());
        repository.save(session);

        // And mock client
        authorizationClient.when(HttpRequest.request().withMethod("POST")
                .withBody(new ParameterBody(new Parameter("token", ACCESS_TOKEN.serialize())))
                .withPath("/ExempleAuthorization/oauth/revoke_token"))
                .respond(HttpResponse.response().withStatusCode(500));

        // When perform post
        Response response = requestSpecification
                .cookie("JSESSIONID", session.getId())
                .post(restTemplate.getRootUri() + "/ExempleAuthorization/oauth/revoke_token");

        // Then check response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());

        // And check session
        assertThat(repository.findById(session.getId())).isNotNull();

    }

}
