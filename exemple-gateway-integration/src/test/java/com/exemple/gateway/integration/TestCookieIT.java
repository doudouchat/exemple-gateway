package com.exemple.gateway.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.exemple.service.api.integration.core.JsonRestTemplate;

import io.restassured.http.ContentType;
import io.restassured.http.Cookie;
import io.restassured.response.Response;

public class TestCookieIT {

    private static final String URL = "/ws/test";

    private Cookie sessionId;
    private Cookie xsrfToken;

    @Test
    public void token() {

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
    public void retryToken() {

        Map<String, String> params = new HashMap<>();
        params.put("grant_type", "client_credentials");

        Response response = JsonRestTemplate.given(JsonRestTemplate.APPLICATION_URL, ContentType.URLENC)

                .cookie("JSESSIONID", sessionId.getValue())

                .auth().basic("resource", "secret").formParams(params).post("/oauth/token");

        assertThat(response.getStatusCode(), is(HttpStatus.OK.value()));

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

    @DataProvider(name = "postFailures")
    private Object[][] postFailures() {

        return new Object[][] {
                //
                { "bad jsessionId", xsrfToken.getValue(), HttpStatus.UNAUTHORIZED },
                //
                { sessionId.getValue(), "bad xrsf token", HttpStatus.FORBIDDEN }

        };
    }

    @Test(dependsOnMethods = "token", dataProvider = "postFailures")
    public void postFailure(String jsessionId, String xrsfToken, HttpStatus expectedStatus) {

        Map<String, Object> body = new HashMap<>();
        body.put("value", UUID.randomUUID());

        Response response = JsonRestTemplate.given()

                .cookie("JSESSIONID", jsessionId).cookie("XSRF-TOKEN", xrsfToken).queryParam("debug", "true")

                .header("X-XSRF-TOKEN", xsrfToken.getValue())

                .body(body).post(URL);

        assertThat(response.getStatusCode(), is(expectedStatus.value()));

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
