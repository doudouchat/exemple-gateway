package com.exemple.gateway.integration.cookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.HashMap;
import java.util.Map;

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
public class TestCookieRefreshIT {

    private static final String URL = "/ws/test";

    private Cookie sessionId;
    private Cookie xsrfToken;

    @Test
    @Order(1)
    public void token() throws JsonProcessingException {

        // When perform post
        Map<String, Object> params = new HashMap<>();
        params.put("grant_type", "password");
        params.put("username", "jean.dupond@gmail.com");
        params.put("password", "123");
        params.put("client_id", "resource");
        params.put("redirect_uri", "xxx");

        Response response = JsonRestTemplate.given(JsonRestTemplate.APPLICATION_URL, ContentType.URLENC).auth().basic("resource", "secret")
                .formParams(params).post("/oauth/token");

        // Then check response
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(200),
                () -> assertThat(response.getCookies()).isNotEmpty(),
                () -> assertThat(response.getCookie("JSESSIONID")).isNotNull(),
                () -> assertThat(response.getCookie("XSRF-TOKEN")).isNotNull());

        sessionId = response.getDetailedCookie("JSESSIONID");
        xsrfToken = response.getDetailedCookie("XSRF-TOKEN");

        // And check session
        assertThat(sessionId.isHttpOnly()).isTrue();

        // And check token
        assertThat(xsrfToken.isHttpOnly()).isFalse();

    }

    @Test
    @Order(2)
    public void get() {

        // When perform get
        Response response = JsonRestTemplate.given()

                .cookie("JSESSIONID", sessionId.getValue()).cookie("XSRF-TOKEN", xsrfToken.getValue()).queryParam("debug", "true")

                .get(URL + "/{id}", "123");

        // Then check response
        assertThat(response.getStatusCode()).isEqualTo(200);

    }

}
