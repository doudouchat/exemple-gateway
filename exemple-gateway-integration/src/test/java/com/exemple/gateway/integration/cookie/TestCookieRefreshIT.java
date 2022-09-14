package com.exemple.gateway.integration.cookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import com.exemple.gateway.integration.common.JsonRestTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;

import io.restassured.http.ContentType;
import io.restassured.http.Cookie;
import io.restassured.response.Response;

@TestMethodOrder(OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestCookieRefreshIT {

    private static final String URL = "/ws/test";

    private static final Pattern LOCATION;

    static {

        LOCATION = Pattern.compile(".*code=([a-zA-Z0-9\\-_]*)(&state=)?(.*)?", Pattern.DOTALL);
    }

    private Cookie sessionId;
    private Cookie xsrfToken;

    @Test
    @Order(1)
    void token() throws JsonProcessingException {

        // Given login

        Response responseLogin = JsonRestTemplate.given(JsonRestTemplate.BROWSER_URL, ContentType.URLENC)
                .formParams("username", "jean.dupond@gmail.com", "password", "123")
                .post("/login");
        assertThat(responseLogin.getStatusCode()).isEqualTo(302);
        String xAuthToken = responseLogin.getHeader("X-Auth-Token");

        // When perform authorize

        Response response = JsonRestTemplate.browser()
                .redirects().follow(false)
                .header("X-Auth-Token", xAuthToken)
                .queryParam("response_type", "code")
                .queryParam("client_id", "resource")
                .queryParam("scope", "test:read")
                .queryParam("redirect_uri", "http://xxx")
                .queryParam("state", "123")
                .get("/oauth/authorize");

        String location = response.getHeader("Location");
        assertThat(response.getStatusCode()).isEqualTo(302);

        Matcher locationMatcher = LOCATION.matcher(location);
        assertThat(locationMatcher.lookingAt()).isTrue();

        String code = locationMatcher.group(1);
        String state = locationMatcher.group(3);

        assertThat(state).isEqualTo("123");

        // And perform token

        Map<String, String> params = Map.of(
                "grant_type", "authorization_code",
                "code", code,
                "client_id", "resource",
                "redirect_uri", "http://xxx");

        Response responseToken = JsonRestTemplate.given(JsonRestTemplate.BROWSER_URL, ContentType.URLENC)
                .header("Authorization", "Basic " + Base64.encodeBase64String("resource:secret".getBytes(StandardCharsets.UTF_8)))
                .formParams(params)
                .post("/oauth/token");

        // Then check response
        assertAll(
                () -> assertThat(responseToken.getStatusCode()).isEqualTo(200),
                () -> assertThat(responseToken.getCookies()).isNotEmpty(),
                () -> assertThat(responseToken.getCookie("JSESSIONID")).isNotNull(),
                () -> assertThat(responseToken.getCookie("XSRF-TOKEN")).isNotNull());

        sessionId = responseToken.getDetailedCookie("JSESSIONID");
        xsrfToken = responseToken.getDetailedCookie("XSRF-TOKEN");

        // And check session
        assertThat(sessionId.isHttpOnly()).isTrue();

        // And check token
        assertThat(xsrfToken.isHttpOnly()).isFalse();

    }

    @Test
    @Order(2)
    void get() {

        // When perform get
        Response response = JsonRestTemplate.browser()
                .cookie("JSESSIONID", sessionId.getValue()).cookie("XSRF-TOKEN", xsrfToken.getValue()).queryParam("debug", "true")
                .get(URL + "/{id}", "123");

        // Then check response
        assertThat(response.getStatusCode()).isEqualTo(200);

    }

}
