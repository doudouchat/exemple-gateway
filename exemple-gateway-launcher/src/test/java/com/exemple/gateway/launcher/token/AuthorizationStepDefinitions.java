package com.exemple.gateway.launcher.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;

import com.exemple.gateway.launcher.common.JsonRestTemplate;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import io.cucumber.java.en.Given;
import io.restassured.http.ContentType;
import io.restassured.http.Cookie;
import io.restassured.response.Response;

public class AuthorizationStepDefinitions {

    private static final Pattern LOCATION = Pattern.compile(".*code=([a-zA-Z0-9\\-_]*)(&state=)?(.*)?", Pattern.DOTALL);

    @Autowired
    private AuthorizationTestContext context;

    @Autowired
    private JWSSigner signer;

    @Given("create access token")
    public void createAccessToken() throws JOSEException {

        var payload = new JWTClaimsSet.Builder()
                .audience("test")
                .claim("scope", new String[] { "test:read", "test:create", "test:delete", "test:update" })
                .build();

        SignedJWT accessToken = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).build(), payload);
        accessToken.sign(signer);

        context.setAccessToken(accessToken.serialize());

    }

    @Given("get access token by client credentials")
    public void tokenByClientCredentials() {

        Map<String, String> params = Map.of("grant_type", "client_credentials");

        Response response = JsonRestTemplate.given(JsonRestTemplate.BROWSER_URL, ContentType.URLENC)
                .header("Authorization", "Basic " + Base64.encodeBase64String("resource:secret".getBytes(StandardCharsets.UTF_8)))
                .formParams(params).post("/oauth/token");

        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(200),
                () -> assertThat(response.getCookies()).isNotEmpty(),
                () -> assertThat(response.getCookie("JSESSIONID")).isNotNull(),
                () -> assertThat(response.getCookie("XSRF-TOKEN")).isNotNull());

        Cookie sessionId = response.getDetailedCookie("JSESSIONID");
        Cookie xsrfToken = response.getDetailedCookie("XSRF-TOKEN");

        context.setSessionId(sessionId);
        context.setXsrfToken(xsrfToken);

    }

    @Given("get access token by authorize")
    public void tokenByAuthorize() {

        // Given login

        Response responseLogin = JsonRestTemplate.given(JsonRestTemplate.BROWSER_URL, ContentType.URLENC)
                .formParams("username", "jean.dupond@gmail.com", "password", "123")
                .post("/login");
        assertThat(responseLogin.getStatusCode()).isEqualTo(200);
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

        Cookie sessionId = responseToken.getDetailedCookie("JSESSIONID");
        Cookie xsrfToken = responseToken.getDetailedCookie("XSRF-TOKEN");

        context.setSessionId(sessionId);
        context.setXsrfToken(xsrfToken);

    }

    @Given("use JSESSIONID {string}")
    public void useJSessionId(String sessionId) {

        context.setSessionId(new Cookie.Builder("JSESSIONID", sessionId).build());

    }

    @Given("use XSRF-TOKEN {string}")
    public void useXsrfToken(String xsrfToken) {

        context.setXsrfToken(new Cookie.Builder("XSRF-TOKEN", xsrfToken).build());

    }

}
