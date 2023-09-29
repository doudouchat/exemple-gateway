package com.exemple.gateway.integration.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthorizationServerTest {

    private static final Pattern LOCATION = Pattern.compile(".*code=([a-zA-Z0-9\\-_]*)(&state=)?(.*)?", Pattern.DOTALL);

    @Autowired
    private TestRestTemplate restTemplate;

    private RequestSpecification requestSpecification;

    @BeforeEach
    public void before() {

        requestSpecification = RestAssured.given();

    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @TestMethodOrder(OrderAnnotation.class)
    class AuthorizationByCode {

        private String xAuthToken;

        private String code;

        private String accessToken;

        @Test
        @Order(1)
        void login() {

            // When perform login

            Response response = requestSpecification
                    .contentType(ContentType.URLENC)
                    .formParams("username", "jean.dupond@gmail.com", "password", "123")
                    .post(restTemplate.getRootUri() + "/login");

            // Then check response

            assertAll(
                    () -> assertThat(response.getStatusCode()).as(response.getBody().asPrettyString()).isEqualTo(HttpStatus.FOUND.value()),
                    () -> assertThat(response.getHeader("X-Auth-Token")).isNotNull(),
                    () -> assertThat(response.getCookies()).isEmpty());

            xAuthToken = response.getHeader("X-Auth-Token");

        }

        @Test
        @Order(2)
        void authorize() {

            // When perform authorize
            Response response = requestSpecification
                    .redirects().follow(false)
                    .header("X-Auth-Token", xAuthToken)
                    .queryParam("response_type", "code")
                    .queryParam("client_id", "resource")
                    .queryParam("scope", "test:create")
                    .queryParam("redirect_uri", "http://xxx")
                    .get(restTemplate.getRootUri() + "/oauth/authorize");

            // Then check response

            assertAll(
                    () -> assertThat(response.getStatusCode()).as(response.getBody().asPrettyString()).isEqualTo(HttpStatus.FOUND.value()),
                    () -> assertThat(response.getHeader(HttpHeaders.LOCATION)).isNotNull());

            String location = response.getHeader(HttpHeaders.LOCATION);

            // And check location

            Matcher locationMatcher = LOCATION.matcher(location);
            assertThat(locationMatcher.lookingAt()).isTrue();

            code = locationMatcher.group(1);

            // And check state

            assertThat(code).isNotEmpty();

        }

        @Test
        @Order(3)
        void token() {

            // When perform get access token

            Map<String, String> params = Map.of(
                    "grant_type", "authorization_code",
                    "code", code,
                    "client_id", "resource",
                    "redirect_uri", "http://xxx");

            Response response = requestSpecification
                    .header("Authorization", "Basic " + Base64.encodeBase64String("resource:secret".getBytes()))
                    .contentType(ContentType.URLENC)
                    .formParams(params)
                    .post(restTemplate.getRootUri() + "/oauth/token");

            // Then check response

            assertAll(
                    () -> assertThat(response.getStatusCode()).as(response.getBody().asPrettyString()).isEqualTo(HttpStatus.OK.value()),
                    () -> assertThat(response.jsonPath().getString("access_token")).isNotNull());

            accessToken = response.jsonPath().getString("access_token");

        }

        @Test
        @Order(4)
        void post() {

            Map<String, Object> body = Map.of("value", UUID.randomUUID());

            Response response = requestSpecification
                    .header("Authorization", "Bearer " + accessToken)
                    .queryParam("debug", "true")
                    .contentType(ContentType.JSON)
                    .body(body)
                    .post(restTemplate.getRootUri() + "/ws/test");

            assertThat(response.getStatusCode()).as(response.getBody().asPrettyString()).isEqualTo(HttpStatus.CREATED.value());
        }

        @Test
        @Order(4)
        void get() {

            Response response = requestSpecification
                    .header("Authorization", "Bearer " + accessToken)
                    .queryParam("debug", "true")
                    .get(restTemplate.getRootUri() + "/ws/test/{id}", "123");

            assertThat(response.getStatusCode()).as(response.getBody().asPrettyString()).isEqualTo(HttpStatus.OK.value());
        }

        @Test
        @Order(4)
        void head() {

            Response response = requestSpecification
                    .header("Authorization", "Bearer " + accessToken)
                    .queryParam("debug", "true")
                    .head(restTemplate.getRootUri() + "/ws/test/{id}", "123");

            assertThat(response.getStatusCode()).as(response.getBody().asPrettyString()).isEqualTo(HttpStatus.NO_CONTENT.value());

        }

        @Test
        @Order(4)
        void delete() {

            Response response = requestSpecification
                    .header("Authorization", "Bearer " + accessToken)
                    .queryParam("debug", "true")
                    .delete(restTemplate.getRootUri() + "/ws/test/{id}", "123");

            assertThat(response.getStatusCode()).as(response.getBody().asPrettyString()).isEqualTo(HttpStatus.NO_CONTENT.value());

        }

        @Test
        @Order(4)
        void patch() {

            Map<String, Object> patch = Map.of(
                    "op", "replace",
                    "path", "/value",
                    "value", UUID.randomUUID());

            Response response = requestSpecification
                    .header("Authorization", "Bearer " + accessToken)
                    .queryParam("debug", "true")
                    .contentType(ContentType.JSON)
                    .body(Collections.singletonList(patch))
                    .patch(restTemplate.getRootUri() + "/ws/test/{id}", "123");

            assertThat(response.getStatusCode()).as(response.getBody().asPrettyString()).isEqualTo(HttpStatus.NO_CONTENT.value());

        }

        @Test
        @Order(4)
        void options() {

            Response response = requestSpecification
                    .header("Authorization", "Bearer " + accessToken)
                    .queryParam("debug", "true")
                    .options(restTemplate.getRootUri() + "/ws/test/{id}", "123");

            assertThat(response.getStatusCode()).as(response.getBody().asPrettyString()).isEqualTo(HttpStatus.OK.value());

        }

    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @TestMethodOrder(OrderAnnotation.class)
    class AuthorizationByCredentials {

        private String accessToken;

        @Test
        @Order(0)
        void credentials() {

            // Given client credentials

            Map<String, String> params = Map.of("grant_type", "client_credentials");

            // When perform get access token

            Response response = requestSpecification
                    .header("Authorization", "Basic " + Base64.encodeBase64String("resource:secret".getBytes()))
                    .formParams(params)
                    .post(restTemplate.getRootUri() + "/oauth/token");

            // Then check response

            assertAll(
                    () -> assertThat(response.getStatusCode()).as(response.getBody().asPrettyString()).isEqualTo(HttpStatus.OK.value()),
                    () -> assertThat(response.jsonPath().getString("access_token")).isNotNull());

            accessToken = response.jsonPath().getString("access_token");

        }

        @Test
        @Order(1)
        void post() {

            Map<String, Object> body = Map.of("value", UUID.randomUUID());

            Response response = requestSpecification
                    .header("Authorization", "Bearer " + accessToken)
                    .queryParam("debug", "true")
                    .contentType(ContentType.JSON)
                    .body(body)
                    .post(restTemplate.getRootUri() + "/ws/test");

            assertThat(response.getStatusCode()).as(response.getBody().asPrettyString()).isEqualTo(HttpStatus.CREATED.value());
        }

        @Test
        @Order(1)
        void get() {

            Response response = requestSpecification
                    .header("Authorization", "Bearer " + accessToken)
                    .queryParam("debug", "true")
                    .get(restTemplate.getRootUri() + "/ws/test/{id}", "123");

            assertThat(response.getStatusCode()).as(response.getBody().asPrettyString()).isEqualTo(HttpStatus.OK.value());
        }

        @Test
        @Order(1)
        void head() {

            Response response = requestSpecification
                    .header("Authorization", "Bearer " + accessToken)
                    .queryParam("debug", "true")
                    .head(restTemplate.getRootUri() + "/ws/test/{id}", "123");

            assertThat(response.getStatusCode()).as(response.getBody().asPrettyString()).isEqualTo(HttpStatus.NO_CONTENT.value());

        }

        @Test
        @Order(1)
        void delete() {

            Response response = requestSpecification
                    .header("Authorization", "Bearer " + accessToken)
                    .queryParam("debug", "true")
                    .delete(restTemplate.getRootUri() + "/ws/test/{id}", "123");

            assertThat(response.getStatusCode()).as(response.getBody().asPrettyString()).isEqualTo(HttpStatus.NO_CONTENT.value());

        }

        @Test
        @Order(1)
        void patch() {

            Map<String, Object> patch = Map.of(
                    "op", "replace",
                    "path", "/value",
                    "value", UUID.randomUUID());

            Response response = requestSpecification
                    .header("Authorization", "Bearer " + accessToken)
                    .queryParam("debug", "true")
                    .contentType(ContentType.JSON)
                    .body(Collections.singletonList(patch))
                    .patch(restTemplate.getRootUri() + "/ws/test/{id}", "123");

            assertThat(response.getStatusCode()).as(response.getBody().asPrettyString()).isEqualTo(HttpStatus.NO_CONTENT.value());

        }

        @Test
        @Order(1)
        void options() {

            Response response = requestSpecification
                    .header("Authorization", "Bearer " + accessToken)
                    .queryParam("debug", "true")
                    .options(restTemplate.getRootUri() + "/ws/test/{id}", "123");

            assertThat(response.getStatusCode()).as(response.getBody().asPrettyString()).isEqualTo(HttpStatus.OK.value());

        }

    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @TestMethodOrder(OrderAnnotation.class)
    class Unautorized {

        private String accessToken;

        @BeforeAll
        public void createAccessToken() throws JOSEException {

            RSAKey rasKey = new RSAKeyGenerator(2048).keyUse(KeyUse.SIGNATURE).generate();

            var payload = new JWTClaimsSet.Builder()
                    .jwtID(UUID.randomUUID().toString())
                    .build();

            var token = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).build(),
                    payload);
            token.sign(new RSASSASigner(rasKey));

            accessToken = token.serialize();

        }

        @Test
        @Order(4)
        void postUnauthorized() {

            Map<String, Object> body = Map.of("value", UUID.randomUUID());

            Response response = requestSpecification
                    .header("Authorization", "Bearer " + accessToken)
                    .queryParam("debug", "true")
                    .contentType(ContentType.JSON)
                    .body(body)
                    .post(restTemplate.getRootUri() + "/ws/test");

            assertThat(response.getStatusCode()).as(response.getBody().asPrettyString()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        }

        @Test
        @Order(4)
        void getUnauthorized() {

            Response response = requestSpecification
                    .header("Authorization", "Bearer " + accessToken)
                    .queryParam("debug", "true")
                    .get(restTemplate.getRootUri() + "/ws/test/{id}", "123");

            assertThat(response.getStatusCode()).as(response.getBody().asPrettyString()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        }

        @Test
        @Order(4)
        void headUnautorized() {

            Response response = requestSpecification
                    .header("Authorization", "Bearer " + accessToken)
                    .queryParam("debug", "true")
                    .head(restTemplate.getRootUri() + "/ws/test/{id}", "123");

            assertThat(response.getStatusCode()).as(response.getBody().asPrettyString()).isEqualTo(HttpStatus.UNAUTHORIZED.value());

        }

        @Test
        @Order(4)
        void deleteUnautorized() {

            Response response = requestSpecification
                    .header("Authorization", "Bearer " + accessToken)
                    .queryParam("debug", "true")
                    .delete(restTemplate.getRootUri() + "/ws/test/{id}", "123");

            assertThat(response.getStatusCode()).as(response.getBody().asPrettyString()).isEqualTo(HttpStatus.UNAUTHORIZED.value());

        }

        @Test
        @Order(4)
        void patchUnautorized() {

            Map<String, Object> patch = Map.of(
                    "op", "replace",
                    "path", "/value",
                    "value", UUID.randomUUID());

            Response response = requestSpecification
                    .header("Authorization", "Bearer " + accessToken)
                    .queryParam("debug", "true")
                    .contentType(ContentType.JSON)
                    .body(Collections.singletonList(patch))
                    .patch(restTemplate.getRootUri() + "/ws/test/{id}", "123");

            assertThat(response.getStatusCode()).as(response.getBody().asPrettyString()).isEqualTo(HttpStatus.UNAUTHORIZED.value());

        }

        @Test
        @Order(4)
        void optionsUnautorized() {

            Response response = requestSpecification
                    .header("Authorization", "Bearer " + accessToken)
                    .queryParam("debug", "true")
                    .options(restTemplate.getRootUri() + "/ws/test/{id}", "123");

            assertThat(response.getStatusCode()).as(response.getBody().asPrettyString()).isEqualTo(HttpStatus.UNAUTHORIZED.value());

        }

    }
}
