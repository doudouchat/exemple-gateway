package com.exemple.gateway.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.testng.annotations.Test;

import com.exemple.service.api.integration.core.JsonRestTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;

import io.restassured.http.ContentType;
import io.restassured.http.Cookie;
import io.restassured.response.Response;

public class TestCookieIT {

    private static final String URL = "/ws/test";

    private Cookie sessionId;
    private Cookie xsrfToken;

    @Test
    public void token() throws JsonProcessingException {

        Map<String, String> params = new HashMap<>();
        params.put("grant_type", "client_credentials");

        Response response = JsonRestTemplate.given(JsonRestTemplate.APPLICATION_URL, ContentType.URLENC).auth().basic("resource", "secret")
                .formParams(params).post("/oauth/token");

        assertThat(response.getStatusCode(), is(HttpStatus.OK.value()));

        assertThat(response.getStatusCode(), is(HttpStatus.OK.value()));
        assertThat(response.getCookies().isEmpty(), is(false));
        assertThat(response.getCookie("JSESSIONID"), is(notNullValue()));
        assertThat(response.getCookie("XSRF-TOKEN"), is(notNullValue()));

        sessionId = response.getDetailedCookie("JSESSIONID");
        xsrfToken = response.getDetailedCookie("XSRF-TOKEN");
        assertThat(sessionId.isHttpOnly(), is(true));
        assertThat(xsrfToken.isHttpOnly(), is(false));

    }

    @Test(dependsOnMethods = "token")
    public void post() {

        Map<String, Object> body = new HashMap<>();
        body.put("value", UUID.randomUUID());

        Response response = JsonRestTemplate.given()

                .cookie("JSESSIONID", sessionId.getValue()).cookie("XSRF-TOKEN", xsrfToken.getValue()).queryParam("debug", "true")

                .header("X-XSRF-TOKEN", xsrfToken.getValue())

                .body(body).post(URL);

        assertThat(response.getStatusCode(), is(HttpStatus.CREATED.value()));

    }

    @Test(dependsOnMethods = "token")
    public void head() {

        Response response = JsonRestTemplate.given()

                .cookie("JSESSIONID", sessionId.getValue()).cookie("XSRF-TOKEN", xsrfToken.getValue()).queryParam("debug", "true")

                .header("X-XSRF-TOKEN", xsrfToken.getValue())

                .head(URL + "/{id}", "123");

        assertThat(response.getStatusCode(), is(HttpStatus.NO_CONTENT.value()));

    }

    @Test(dependsOnMethods = "token")
    public void get() {

        Response response = JsonRestTemplate.given()

                .cookie("JSESSIONID", sessionId.getValue()).cookie("XSRF-TOKEN", xsrfToken.getValue()).queryParam("debug", "true")

                .get(URL + "/{id}", "123");

        assertThat(response.getStatusCode(), is(HttpStatus.OK.value()));

    }

    @Test(dependsOnMethods = "token")
    public void delete() {

        Response response = JsonRestTemplate.given()

                .cookie("JSESSIONID", sessionId.getValue()).cookie("XSRF-TOKEN", xsrfToken.getValue()).queryParam("debug", "true")

                .header("X-XSRF-TOKEN", xsrfToken.getValue())

                .delete(URL + "/{id}", "123");

        assertThat(response.getStatusCode(), is(HttpStatus.NO_CONTENT.value()));

    }

    @Test(dependsOnMethods = "token")
    public void patch() {

        Map<String, Object> patch = new HashMap<>();
        patch.put("op", "replace");
        patch.put("path", "/value");
        patch.put("value", UUID.randomUUID());

        Response response = JsonRestTemplate.given()

                .cookie("JSESSIONID", sessionId.getValue()).cookie("XSRF-TOKEN", xsrfToken.getValue()).queryParam("debug", "true")

                .header("X-XSRF-TOKEN", xsrfToken.getValue())

                .body(Collections.singletonList(patch)).patch(URL + "/{id}", "123");

        assertThat(response.getStatusCode(), is(HttpStatus.NO_CONTENT.value()));

    }

    @Test(dependsOnMethods = "token")
    public void options() {

        Response response = JsonRestTemplate.given()

                .cookie("JSESSIONID", sessionId.getValue()).cookie("XSRF-TOKEN", xsrfToken.getValue()).queryParam("debug", "true")

                .options(URL + "/{id}", "123");

        assertThat(response.getStatusCode(), is(HttpStatus.OK.value()));

    }
}
