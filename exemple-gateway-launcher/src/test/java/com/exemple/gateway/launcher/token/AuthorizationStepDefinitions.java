package com.exemple.gateway.launcher.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import com.exemple.gateway.launcher.common.JsonRestTemplate;

import io.cucumber.java.en.Given;
import io.restassured.http.ContentType;
import io.restassured.http.Cookie;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

public class AuthorizationStepDefinitions {

    @Autowired
    private AuthorizationTestContext context;

    @Given("get csrf token")
    public void tokenCsrf() {

        Map<String, String> params = Map.of("grant_type", "client_credentials");

        Response response = JsonRestTemplate.given(JsonRestTemplate.BROWSER_URL, ContentType.URLENC)
                .formParams(params).get("/actuator/health");

        assertAll(
                () -> assertThat(response.getStatusCode()).as(response.asPrettyString()).isEqualTo(200),
                () -> assertThat(response.getCookies()).isNotEmpty(),
                () -> assertThat(response.getCookie("XSRF-TOKEN")).isNotNull());
        Cookie xsrfToken = response.getDetailedCookie("XSRF-TOKEN");

        context.setXsrfToken(xsrfToken);

    }

    @Given("login")
    public void login() {

        Response responseLogin = JsonRestTemplate.given(JsonRestTemplate.BROWSER_URL, ContentType.URLENC)
                .formParams("username", "jean.dupond@gmail.com", "password", "123")
                .post("/login");
        assertThat(responseLogin.getStatusCode()).isEqualTo(200);
        String xAuthToken = responseLogin.getHeader("X-Auth-Token");

        context.setXAuthToken(xAuthToken);

    }

    @Given("authorize by browser")
    public void authorizeByBrowser() {

        authorize(JsonRestTemplate.browser());

    }

    @Given("authorize by application")
    public void authorizeByApplication() {

        authorize(JsonRestTemplate.api());

    }

    private void authorize(RequestSpecification request) {

        var location = URI.create(context.getLocation());
        var queryParams = Arrays.stream(location.getQuery().split("&"))
                .map(AuthorizationStepDefinitions::decode)
                .map(query -> query.split("="))
                .collect(Collectors.toMap(query -> query[0], query -> query[1]));

        Response response = request
                .redirects().follow(false)
                .cookie("JSESSIONID", context.getSessionId().getValue())
                .header("X-Auth-Token", context.getXAuthToken())
                .queryParams(queryParams)
                .get(location);

        assertThat(response.getStatusCode()).isEqualTo(200);

        context.setLocation(response.getHeader("Location"));

    }

    @Given("token by browser")
    public void tokenByBrowser() {

        var location = URI.create(context.getLocation());
        var queryParams = Arrays.stream(location.getQuery().split("&"))
                .map(AuthorizationStepDefinitions::decode)
                .map(query -> query.split("="))
                .collect(Collectors.toMap(query -> query[0], query -> query[1]));

        queryParams.put("state", queryParams.get("state") + "=");

        Response response = JsonRestTemplate.given(context.getLocation(), ContentType.JSON)
                .redirects().follow(false)
                .cookie("JSESSIONID", context.getSessionId().getValue())
                .cookie("XSRF-TOKEN", context.getXsrfToken().getValue())
                .header("X-XSRF-TOKEN", context.getXsrfToken().getValue())
                .queryParams(queryParams)
                .get(location);

        assertThat(response.getStatusCode()).isEqualTo(302);
        assertThat(response.getHeader("Location")).isEqualTo("/");

    }

    @Given("token by application")
    public void tokenByApplication() {

        var location = URI.create(context.getLocation());
        var queryParams = Arrays.stream(location.getQuery().split("&"))
                .map(AuthorizationStepDefinitions::decode)
                .map(query -> query.split("="))
                .collect(Collectors.toMap(query -> query[0], query -> query[1]));

        queryParams.put("state", queryParams.get("state") + "=");

        Response response = JsonRestTemplate.given(context.getLocation(), ContentType.JSON)
                .redirects().follow(false)
                .cookie("JSESSIONID", context.getSessionId().getValue())
                .queryParams(queryParams)
                .get(location);

        assertThat(response.getStatusCode()).isEqualTo(302);
        assertThat(response.getHeader("Location")).isEqualTo("/");

    }

    @Given("clear JSESSIONID")
    public void clearJSessionId() {

        context.setSessionId(new Cookie.Builder("JSESSIONID", null).setMaxAge(0).build());

    }

    @Given("use XSRF-TOKEN {string}")
    public void useXsrfToken(String xsrfToken) {

        context.setXsrfToken(new Cookie.Builder("XSRF-TOKEN", xsrfToken).build());

    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

}
