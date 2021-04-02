package com.exemple.gateway.integration.cookie;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;

import com.exemple.gateway.integration.common.JsonRestTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;

import io.restassured.http.ContentType;
import io.restassured.http.Cookie;
import io.restassured.response.Response;

public class TestCookieRefreshIT {

    private static final String URL = "/ws/test";

    private Cookie sessionId;
    private Cookie xsrfToken;

    @Test
    public void token() throws JsonProcessingException {

        Map<String, Object> params = new HashMap<>();
        params.put("grant_type", "password");
        params.put("username", "jean.dupond@gmail.com");
        params.put("password", "123");
        params.put("client_id", "resource");
        params.put("redirect_uri", "xxx");

        Response response = JsonRestTemplate.given(JsonRestTemplate.APPLICATION_URL, ContentType.URLENC).auth().basic("resource", "secret")
                .formParams(params).post("/oauth/token");

        assertThat(response.getStatusCode(), is(200));
        assertThat(response.getCookies().isEmpty(), is(false));
        assertThat(response.getCookie("JSESSIONID"), is(notNullValue()));
        assertThat(response.getCookie("XSRF-TOKEN"), is(notNullValue()));

        sessionId = response.getDetailedCookie("JSESSIONID");
        xsrfToken = response.getDetailedCookie("XSRF-TOKEN");
        assertThat(sessionId.isHttpOnly(), is(true));
        assertThat(xsrfToken.isHttpOnly(), is(false));

    }

    @Test(dependsOnMethods = "token")
    public void get() {

        Response response = JsonRestTemplate.given()

                .cookie("JSESSIONID", sessionId.getValue()).cookie("XSRF-TOKEN", xsrfToken.getValue()).queryParam("debug", "true")

                .get(URL + "/{id}", "123");

        assertThat(response.getStatusCode(), is(200));

    }

}
