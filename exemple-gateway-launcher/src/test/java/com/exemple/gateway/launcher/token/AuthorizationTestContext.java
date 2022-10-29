package com.exemple.gateway.launcher.token;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.stereotype.Component;

import io.cucumber.spring.ScenarioScope;
import io.restassured.http.Cookie;

@Component
@ScenarioScope
public class AuthorizationTestContext {

    private String accessToken;

    private Cookie sessionId;

    private Cookie xsrfToken;

    public String getAccessToken() {
        assertThat(this.accessToken).as("no access token").isNotNull();
        return this.accessToken;
    }

    public void setAccessToken(String token) {
        this.accessToken = token;
    }

    public Cookie getSessionId() {
        assertThat(this.sessionId).as("no session id").isNotNull();
        return sessionId;
    }

    public void setSessionId(Cookie sessionId) {
        this.sessionId = sessionId;
    }

    public Cookie getXsrfToken() {
        assertThat(this.xsrfToken).as("no xsrf id").isNotNull();
        return xsrfToken;
    }

    public void setXsrfToken(Cookie xsrfToken) {
        this.xsrfToken = xsrfToken;
    }

}
