package com.exemple.gateway.launcher.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import com.exemple.gateway.launcher.common.JsonRestTemplate;
import com.exemple.gateway.launcher.token.AuthorizationTestContext;

import io.cucumber.java.en.When;
import io.restassured.response.Response;

public class ApiStepDefinitions {

    private static final String URL = "/ws/test";

    @Autowired
    private ApiTestContext context;

    @Autowired
    private AuthorizationTestContext authorizationContext;

    @When("perform post with Authorization")
    public void postWithAuthorization() {

        Map<String, Object> body = Map.of("value", UUID.randomUUID());

        Response response = JsonRestTemplate.api()
                .header("Authorization", "Bearer " + authorizationContext.getAccessToken())
                .queryParam("debug", "true")
                .body(body).post(URL);

        context.setResponse(response);
    }

    @When("perform post with JSESSIONID")
    public void postWithJSessionId() {

        Map<String, Object> body = Map.of("value", UUID.randomUUID());

        Response response = JsonRestTemplate.browser()
                .cookie("JSESSIONID", authorizationContext.getSessionId().getValue())
                .cookie("XSRF-TOKEN", authorizationContext.getXsrfToken().getValue())
                .header("X-XSRF-TOKEN", authorizationContext.getXsrfToken().getValue())
                .queryParam("debug", "true")
                .body(body).post(URL);

        context.setResponse(response);
    }

    @When("perform get with Authorization")
    public void getWithAuthorization() {

        Response response = JsonRestTemplate.api()
                .header("Authorization", "Bearer " + authorizationContext.getAccessToken())
                .queryParam("debug", "true")
                .get(URL + "/{id}", "123");

        context.setResponse(response);
    }

    @When("perform get with JSESSIONID")
    public void getWithJSessionId() {

        Response response = JsonRestTemplate.browser()
                .cookie("JSESSIONID", authorizationContext.getSessionId().getValue())
                .cookie("XSRF-TOKEN", authorizationContext.getXsrfToken().getValue())
                .queryParam("debug", "true")
                .get(URL + "/{id}", "123");

        context.setResponse(response);
    }

    @When("perform head with Authorization")
    public void headWithAuthorization() {

        Response response = JsonRestTemplate.api()
                .header("Authorization", "Bearer " + authorizationContext.getAccessToken())
                .queryParam("debug", "true")
                .head(URL + "/{id}", "123");

        context.setResponse(response);

    }

    @When("perform head with JSESSIONID")
    public void headWithJSessionId() {

        Response response = JsonRestTemplate.browser()
                .cookie("JSESSIONID", authorizationContext.getSessionId().getValue())
                .cookie("XSRF-TOKEN", authorizationContext.getXsrfToken().getValue())
                .queryParam("debug", "true")
                .head(URL + "/{id}", "123");

        context.setResponse(response);

    }

    @When("perform delete with Authorization")
    public void deleteWithAuthorization() {

        Response response = JsonRestTemplate.api()
                .header("Authorization", "Bearer " + authorizationContext.getAccessToken())
                .queryParam("debug", "true")
                .delete(URL + "/{id}", "123");

        context.setResponse(response);

    }

    @When("perform delete with JSESSIONID")
    public void deleteWithJSessionId() {

        Response response = JsonRestTemplate.browser()
                .cookie("JSESSIONID", authorizationContext.getSessionId().getValue())
                .cookie("XSRF-TOKEN", authorizationContext.getXsrfToken().getValue())
                .header("X-XSRF-TOKEN", authorizationContext.getXsrfToken().getValue())
                .queryParam("debug", "true")
                .delete(URL + "/{id}", "123");

        context.setResponse(response);

    }

    @When("perform patch with Authorization")
    public void patchWithAuthorization() {

        Map<String, Object> patch = Map.of(
                "op", "replace",
                "path", "/value",
                "value", UUID.randomUUID());

        Response response = JsonRestTemplate.api()
                .header("Authorization", "Bearer " + authorizationContext.getAccessToken())
                .queryParam("debug", "true")
                .body(Collections.singletonList(patch)).patch(URL + "/{id}", "123");

        context.setResponse(response);

    }

    @When("perform patch with JSESSIONID")
    public void patchWithJSessionId() {

        Map<String, Object> patch = Map.of(
                "op", "replace",
                "path", "/value",
                "value", UUID.randomUUID());

        Response response = JsonRestTemplate.browser()
                .cookie("JSESSIONID", authorizationContext.getSessionId().getValue())
                .cookie("XSRF-TOKEN", authorizationContext.getXsrfToken().getValue())
                .header("X-XSRF-TOKEN", authorizationContext.getXsrfToken().getValue())
                .queryParam("debug", "true")
                .body(Collections.singletonList(patch)).patch(URL + "/{id}", "123");

        context.setResponse(response);

    }

    @When("perform options with Authorization")
    public void optionsWithAuthorization() {

        Response response = JsonRestTemplate.api()
                .header("Authorization", "Bearer " + authorizationContext.getAccessToken())
                .queryParam("debug", "true")
                .options(URL + "/{id}", "123");

        context.setResponse(response);

    }

    @When("perform options with JSESSIONID")
    public void optionsWithJSessionId() {

        Response response = JsonRestTemplate.api()
                .cookie("JSESSIONID", authorizationContext.getSessionId().getValue())
                .cookie("XSRF-TOKEN", authorizationContext.getXsrfToken().getValue())
                .queryParam("debug", "true")
                .options(URL + "/{id}", "123");

        context.setResponse(response);

    }

    @When("response status is {int}")
    public void checkResponse(int status) {

        assertThat(context.getResponse().getStatusCode()).isEqualTo(status);
    }
}
