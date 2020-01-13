package com.exemple.gateway.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.mockserver.client.MockServerClient;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.JsonBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.exemple.gateway.common.LoggingFilter;
import com.exemple.gateway.core.GatewayServerTestConfiguration;
import com.exemple.gateway.core.GatewayTestConfiguration;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.restassured.RestAssured;
import io.restassured.http.Cookie;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

@SpringBootTest(classes = { GatewayTestConfiguration.class, GatewayServerTestConfiguration.class }, webEnvironment = WebEnvironment.RANDOM_PORT)
public class SecurityCookieTest extends AbstractTestNGSpringContextTests {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityCookieTest.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private TestRestTemplate restTemplate;

    @Value("${api.port}")
    private int apiPort;

    @Value("${authorization.port}")
    private int authorizationPort;

    private RequestSpecification requestSpecification;

    @Autowired
    private MockServerClient apiClient;

    @Autowired
    private MockServerClient authorizationClient;

    private static final Algorithm HMAC256_ALGORITHM;

    private Cookie sessionId;
    private Cookie xsrfToken;

    static {

        HMAC256_ALGORITHM = Algorithm.HMAC256("abc");

    }

    @BeforeMethod
    private void before() {

        requestSpecification = RestAssured.given().filters(new LoggingFilter(LOG));

        apiClient.reset();
        authorizationClient.reset();

    }

    @Test
    public void token() throws JsonProcessingException {

        String accessToken = JWT.create().withClaim("user_name", "john_doe").withAudience("test")
                .withExpiresAt(Date.from(Instant.now().plus(1, ChronoUnit.DAYS))).withArrayClaim("scope", new String[] { "account:read" })
                .sign(HMAC256_ALGORITHM);

        String refreshToken = JWT.create().withClaim("user_name", "john_doe").withAudience("test")
                .withArrayClaim("scope", new String[] { "account:read" }).sign(HMAC256_ALGORITHM);

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("access_token", accessToken);
        responseBody.put("refresh_token", refreshToken);
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
        assertThat(xsrfToken.isHttpOnly(), is(false));

    }

    @Test(dependsOnMethods = "token")
    public void securitySuccess() {

        apiClient.when(HttpRequest.request().withMethod("POST").withPath("/ExempleService/account"))
                .respond(HttpResponse.response().withHeaders(new Header("Content-Type", "application/json;charset=UTF-8"))
                        .withBody(JsonBody.json(Collections.singletonMap("name", "jean"))).withStatusCode(200));

        Response response = requestSpecification.cookie("JSESSIONID", sessionId.getValue()).cookie("XSRF-TOKEN", xsrfToken.getValue())
                .header("X-XSRF-TOKEN", xsrfToken.getValue()).post(restTemplate.getRootUri() + "/ExempleService/account");

        assertThat(response.getStatusCode(), is(HttpStatus.OK.value()));
        assertThat(response.jsonPath().get("name"), is("jean"));

    }

    @Test(dependsOnMethods = "token")
    public void securityFailure() {

        apiClient.when(HttpRequest.request().withMethod("POST").withPath("/ExempleService/account"))
                .respond(HttpResponse.response().withHeaders(new Header("Content-Type", "application/json;charset=UTF-8"))
                        .withBody(JsonBody.json(Collections.singletonMap("name", "jean"))).withStatusCode(200));

        Response response = requestSpecification.cookie("JSESSIONID", sessionId.getValue()).cookie("XSRF-TOKEN", xsrfToken.getValue())
                .header("X-XSRF-TOKEN", "toto").post(restTemplate.getRootUri() + "/ExempleService/account");

        assertThat(response.getStatusCode(), is(HttpStatus.FORBIDDEN.value()));

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
