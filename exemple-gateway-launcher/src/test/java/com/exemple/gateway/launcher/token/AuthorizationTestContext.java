package com.exemple.gateway.launcher.token;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.stereotype.Component;

import io.cucumber.spring.ScenarioScope;
import io.restassured.http.Cookie;

@Component
@ScenarioScope
public class AuthorizationTestContext {

    private Cookie sessionId;

    private Cookie xsrfToken;

    private String xAuthToken;

    private String location;

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

    public String getXAuthToken() {
        assertThat(this.xAuthToken).as("no xAuthToken").isNotNull();
        return xAuthToken;
    }

    public void setXAuthToken(String xAuthToken) {
        this.xAuthToken = xAuthToken;
    }

    public String getLocation() {
        assertThat(this.location).as("no location").isNotNull();
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

}
