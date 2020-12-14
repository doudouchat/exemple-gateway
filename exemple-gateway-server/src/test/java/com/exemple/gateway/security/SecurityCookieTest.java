package com.exemple.gateway.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.JsonBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.exemple.gateway.common.LoggingFilter;
import com.exemple.gateway.core.GatewayServerTestConfiguration;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.restassured.RestAssured;
import io.restassured.http.Cookie;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

@ActiveProfiles("browser")
public class SecurityCookieTest extends GatewayServerTestConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityCookieTest.class);

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
    private Clock clock;

    @BeforeClass
    public void init() {

        clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());

        ACCESS_TOKEN = JWT.create().withClaim("user_name", "john_doe").withAudience("test")
                .withExpiresAt(Date.from(Instant.now(clock).plus(1, ChronoUnit.DAYS))).withArrayClaim("scope", new String[] { "account:read" })
                .sign(algo);

        DEPRECATED_ACCESS_TOKEN = JWT.create().withClaim("user_name", "john_doe").withAudience("test")
                .withExpiresAt(Date.from(Instant.now(clock).minus(1, ChronoUnit.DAYS))).withArrayClaim("scope", new String[] { "account:read" })
                .sign(algo);

        REFRESH_TOKEN = JWT.create().withClaim("user_name", "john_doe").withAudience("test").withArrayClaim("scope", new String[] { "account:read" })
                .sign(algo);

    }

    @BeforeMethod
    private void before() {

        requestSpecification = RestAssured.given().filters(new LoggingFilter(LOG));

        apiClient.reset();
        authorizationClient.reset();

    }

    @Test
    public void token() throws JsonProcessingException {

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("access_token", ACCESS_TOKEN);
        responseBody.put("refresh_token", REFRESH_TOKEN);
        responseBody.put("scope", "account:read");

        authorizationClient.when(HttpRequest.request().withMethod("POST").withPath("/ExempleAuthorization/oauth/token"))
                .respond(HttpResponse.response().withBody(MAPPER.writeValueAsString(responseBody)).withStatusCode(200));

        Response response = requestSpecification.post(restTemplate.getRootUri() + "/ExempleAuthorization/oauth/token");

        assertThat(response.getStatusCode(), is(HttpStatus.OK.value()));
        assertThat(response.getCookies().isEmpty(), is(false));
        assertThat(response.getCookie("JSESSIONID"), is(notNullValue()));
        assertThat(response.jsonPath().get("scope"), is("account:read"));

        sessionId = response.getDetailedCookie("JSESSIONID");
        xsrfToken = response.getDetailedCookie("XSRF-TOKEN");

        assertThat(sessionId.isHttpOnly(), is(true));
        assertThat(sessionId.getExpiryDate(), is(notNullValue()));
        assertThat(sessionId.getMaxAge(), is(greaterThan(0)));
        assertThat(xsrfToken.isHttpOnly(), is(false));

    }

    @Test(dependsOnMethods = "token")
    public void securitySuccess() {

        apiClient
                .when(HttpRequest.request().withMethod("POST").withHeader("Authorization", "BEARER " + ACCESS_TOKEN)
                        .withPath("/ExempleService/account"))
                .respond(HttpResponse.response().withHeaders(new Header("Content-Type", "application/json;charset=UTF-8"))
                        .withBody(JsonBody.json(Collections.singletonMap("name", "jean"))).withStatusCode(200));

        Response response = requestSpecification.cookie("JSESSIONID", sessionId.getValue()).cookie("XSRF-TOKEN", xsrfToken.getValue())
                .header("X-XSRF-TOKEN", xsrfToken.getValue()).post(restTemplate.getRootUri() + "/ExempleService/account");

        assertThat(response.getStatusCode(), is(HttpStatus.OK.value()));
        assertThat(response.jsonPath().get("name"), is("jean"));

    }

    @Test
    public void securitySuccessWithSessionIdInHeader() throws JsonProcessingException {

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("access_token", ACCESS_TOKEN);
        responseBody.put("refresh_token", REFRESH_TOKEN);
        responseBody.put("scope", "account:read");

        authorizationClient.when(HttpRequest.request().withMethod("POST").withPath("/ExempleAuthorization/oauth/token"))
                .respond(HttpResponse.response().withBody(MAPPER.writeValueAsString(responseBody)).withStatusCode(200));

        Response response = requestSpecification.cookie("JSESSIONID", UUID.randomUUID())
                .post(restTemplate.getRootUri() + "/ExempleAuthorization/oauth/token");

        Cookie sessionId = response.getDetailedCookie("JSESSIONID");
        Cookie xsrfToken = response.getDetailedCookie("XSRF-TOKEN");

        apiClient
                .when(HttpRequest.request().withMethod("POST").withHeader("Authorization", "BEARER " + ACCESS_TOKEN)
                        .withPath("/ExempleService/account"))
                .respond(HttpResponse.response().withHeaders(new Header("Content-Type", "application/json;charset=UTF-8"))
                        .withBody(JsonBody.json(Collections.singletonMap("name", "jean"))).withStatusCode(200));

        requestSpecification = RestAssured.given().filters(new LoggingFilter(LOG));

        response = requestSpecification.cookie("JSESSIONID", sessionId.getValue()).cookie("XSRF-TOKEN", xsrfToken.getValue())
                .header("X-XSRF-TOKEN", xsrfToken.getValue()).post(restTemplate.getRootUri() + "/ExempleService/account");

        assertThat(response.getStatusCode(), is(HttpStatus.OK.value()));
        assertThat(response.jsonPath().get("name"), is("jean"));

    }

    @DataProvider(name = "securityFailures")
    private Object[][] securityFailures() {

        return new Object[][] {

                { DEPRECATED_ACCESS_TOKEN, xsrfToken.getValue(), HttpStatus.UNAUTHORIZED },

                { ACCESS_TOKEN, "toto", HttpStatus.FORBIDDEN } };
    }

    @Test(dataProvider = "securityFailures", dependsOnMethods = "token")
    public void securityFailure(String accessToken, String csrfToken, HttpStatus expectedHttpStatus) throws JsonProcessingException {

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("access_token", accessToken);
        responseBody.put("refresh_token", REFRESH_TOKEN);
        responseBody.put("scope", "account:read");

        authorizationClient.when(HttpRequest.request().withMethod("POST").withPath("/ExempleAuthorization/oauth/token"))
                .respond(HttpResponse.response().withBody(MAPPER.writeValueAsString(responseBody)).withStatusCode(HttpStatus.OK.value()));

        Response response = requestSpecification.post(restTemplate.getRootUri() + "/ExempleAuthorization/oauth/token");

        Cookie sessionId = response.getDetailedCookie("JSESSIONID");

        response = requestSpecification.cookie("JSESSIONID", sessionId.getValue()).cookie("XSRF-TOKEN", xsrfToken.getValue())
                .header("X-XSRF-TOKEN", csrfToken).post(restTemplate.getRootUri() + "/ExempleService/account");

        assertThat(response.getStatusCode(), is(expectedHttpStatus.value()));

    }

    @Test(dependsOnMethods = "token")
    public void securityFailure() throws JsonProcessingException {

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("access_token", ACCESS_TOKEN);

        authorizationClient.when(HttpRequest.request().withMethod("POST").withPath("/ExempleAuthorization/oauth/token"))
                .respond(HttpResponse.response().withBody(MAPPER.writeValueAsString(responseBody)).withStatusCode(HttpStatus.UNAUTHORIZED.value()));

        Response response = requestSpecification.post(restTemplate.getRootUri() + "/ExempleAuthorization/oauth/token");

        Cookie sessionId = response.getDetailedCookie("JSESSIONID");

        assertThat(sessionId, is(nullValue()));

        response = requestSpecification.cookie("XSRF-TOKEN", xsrfToken.getValue()).header("X-XSRF-TOKEN", xsrfToken.getValue())
                .post(restTemplate.getRootUri() + "/ExempleService/account");

        assertThat(response.getStatusCode(), is(HttpStatus.UNAUTHORIZED.value()));

    }

    @Test
    public void tokenEmpty() throws JsonProcessingException {

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("scope", "account:read");

        authorizationClient.when(HttpRequest.request().withMethod("POST").withPath("/ExempleAuthorization/oauth/token"))
                .respond(HttpResponse.response().withBody(MAPPER.writeValueAsString(responseBody)).withStatusCode(200));

        Response response = requestSpecification.post(restTemplate.getRootUri() + "/ExempleAuthorization/oauth/token");

        assertThat(response.getStatusCode(), is(HttpStatus.OK.value()));
        assertThat(response.getCookies().isEmpty(), is(false));
        assertThat(response.getCookie("JSESSIONID"), is(notNullValue()));
        assertThat(response.jsonPath().get("scope"), is("account:read"));

    }

    @Test
    public void tokenFailure() {

        authorizationClient.when(HttpRequest.request().withMethod("POST").withPath("/ExempleAuthorization/oauth/token"))
                .respond(HttpResponse.response().withBody("toto").withStatusCode(200));

        Response response = requestSpecification.post(restTemplate.getRootUri() + "/ExempleAuthorization/oauth/token");

        assertThat(response.getStatusCode(), is(HttpStatus.INTERNAL_SERVER_ERROR.value()));

    }

}
