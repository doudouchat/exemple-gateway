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

import com.exemple.gateway.core.GatewayServerTestConfiguration;
import com.exemple.gateway.core.common.LoggingFilter;
import com.exemple.gateway.core.security.token.validator.NotBlackListTokenValidator;
import com.hazelcast.core.HazelcastInstance;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

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
    private HazelcastInstance hazelcastInstance;

    @Autowired
    private RSASSASigner signer;

    @BeforeEach
    private void before() {

        requestSpecification = RestAssured.given().filters(new LoggingFilter(LOG));

        apiClient.reset();

    }

    @Test
    void securitySuccess() throws JOSEException {

        // Given build token
        var payload = new JWTClaimsSet.Builder()
                .jwtID(UUID.randomUUID().toString())
                .claim("user_name", "john_doe")
                .audience("test")
                .claim("scope", new String[] { "account:read" })
                .build();

        var token = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).build(), payload);
        token.sign(signer);

        // And mock client
        apiClient
                .when(HttpRequest.request()
                        .withMethod("POST")
                        .withHeader("Authorization", "BEARER " + token.serialize())
                        .withPath("/ExempleService/account"))
                .respond(HttpResponse.response()
                        .withHeaders(new Header("Content-Type", "application/json;charset=UTF-8"))
                        .withBody(JsonBody.json(Collections.singletonMap("name", "jean")))
                        .withStatusCode(200));

        // When perform header
        Response response = requestSpecification
                .header("Authorization", "BEARER " + token.serialize())
                .post(restTemplate.getRootUri() + "/ExempleService/account");

        // Then check response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value());

    }

    @Test
    void securityFailureExpiredTime() throws JOSEException {

        // Given build token
        var payload = new JWTClaimsSet.Builder()
                .jwtID(UUID.randomUUID().toString())
                .claim("user_name", "john_doe")
                .audience("test")
                .expirationTime(Date.from(Instant.now().minus(1, ChronoUnit.DAYS)))
                .claim("scope", new String[] { "account:read" })
                .build();

        var token = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).build(), payload);
        token.sign(signer);

        // When perform header
        Response response = requestSpecification
                .header("Authorization", "BEARER " + token.serialize())
                .post(restTemplate.getRootUri() + "/ExempleService/account");

        // Then check response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());

        // And check error
        assertThat(response.getHeader("WWW-Authenticate")).contains("error_description=\"Jwt expired");

    }

    @Test
    void securityFailureTokenInBlackList() throws JOSEException {

        // Given build token
        String jwtId = UUID.randomUUID().toString();
        var payload = new JWTClaimsSet.Builder()
                .jwtID(jwtId)
                .claim("user_name", "john_doe")
                .audience("test")
                .claim("scope", new String[] { "account:read" })
                .build();

        var token = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).build(), payload);
        token.sign(signer);

        hazelcastInstance.getMap(NotBlackListTokenValidator.TOKEN_BLACK_LIST).put(jwtId, token.serialize());

        // When perform header
        Response response = requestSpecification
                .header("Authorization", "BEARER " + token.serialize())
                .post(restTemplate.getRootUri() + "/ExempleService/account");

        // Then check response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());

        // And check body
        assertThat(response.getHeader("WWW-Authenticate")).contains("error_description=\"Jwt is excluded\"");

    }

    @Test
    void securityFailurePublicKeyDoesntCheckSignature() throws JOSEException {

        // Given build token
        String jwtId = UUID.randomUUID().toString();
        var payload = new JWTClaimsSet.Builder()
                .jwtID(jwtId)
                .claim("user_name", "john_doe")
                .audience("test")
                .claim("scope", new String[] { "account:read" })
                .build();

        var token = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).build(), payload);
        RSASSASigner signer = new RSASSASigner(new RSAKeyGenerator(2048).keyUse(KeyUse.SIGNATURE).generate());
        token.sign(signer);

        // When perform header
        Response response = requestSpecification.header("Authorization", "BEARER " + token.serialize())
                .post(restTemplate.getRootUri() + "/ExempleService/account");

        // Then check response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());

        // And check body
        assertThat(response.getHeader("WWW-Authenticate")).contains("error_description=\"Failed to validate the token\"");

    }

}
