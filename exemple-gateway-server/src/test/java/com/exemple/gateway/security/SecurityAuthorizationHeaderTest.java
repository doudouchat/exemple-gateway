package com.exemple.gateway.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;

import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.JsonBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.exemple.gateway.common.LoggingFilter;
import com.exemple.gateway.core.GatewayServerTestConfiguration;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

public class SecurityAuthorizationHeaderTest extends GatewayServerTestConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityAuthorizationHeaderTest.class);

    @Autowired
    private TestRestTemplate restTemplate;

    private RequestSpecification requestSpecification;

    @Autowired
    private Algorithm algo;

    @BeforeMethod
    private void before() {

        requestSpecification = RestAssured.given().filters(new LoggingFilter(LOG));

        apiClient.reset();

    }

    @Test
    public void securitySuccess() {

        String token = JWT.create().withClaim("user_name", "john_doe").withAudience("test").withArrayClaim("scope", new String[] { "account:read" })
                .sign(algo);

        apiClient.when(HttpRequest.request().withMethod("POST").withHeader("Authorization", "BEARER " + token).withPath("/ExempleService/account"))
                .respond(HttpResponse.response().withHeaders(new Header("Content-Type", "application/json;charset=UTF-8"))
                        .withBody(JsonBody.json(Collections.singletonMap("name", "jean"))).withStatusCode(200));

        Response response = requestSpecification.header("Authorization", "BEARER " + token)
                .post(restTemplate.getRootUri() + "/ExempleService/account");

        assertThat(response.getStatusCode(), is(HttpStatus.OK.value()));

    }

    @Test
    public void securityFailure() {

        String token = JWT.create().withClaim("user_name", "john_doe").withAudience("test").withArrayClaim("scope", new String[] { "account:read" })
                .withExpiresAt(Date.from(Instant.now().minus(1, ChronoUnit.DAYS))).sign(algo);

        Response response = requestSpecification.header("Authorization", "BEARER " + token)
                .post(restTemplate.getRootUri() + "/ExempleService/account");

        assertThat(response.getStatusCode(), is(HttpStatus.UNAUTHORIZED.value()));

    }

}
