package com.exemple.gateway.launcher.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import com.exemple.gateway.launcher.common.JsonRestTemplate;
import com.exemple.gateway.launcher.token.AuthorizationTestContext;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

public class ApiStepDefinitions {

    private static final String URL = "/ws/test";

    @Autowired
    private ApiTestContext context;

    @Autowired
    private AuthorizationTestContext authorizationContext;

    @When("perform post by application")
    public void postByApplication() {

        Map<String, Object> body = Map.of("value", UUID.randomUUID());

        Response response = JsonRestTemplate.api()
                .queryParam("debug", "true")
                .body(body).post(URL);

        context.setResponse(response);
    }

    @When("perform post by browser")
    public void postByBrowser() {

        Map<String, Object> body = Map.of("value", UUID.randomUUID());

        Response response = builderBrowser()
                .body(body).post(URL);

        context.setResponse(response);
    }

    @When("perform get by application")
    public void getByApplication() {

        Response response = JsonRestTemplate.api()
                .queryParam("debug", "true")
                .get(URL + "/{id}", "123");

        context.setResponse(response);
        authorizationContext.setLocation(response.getHeader("Location"));
        authorizationContext.setSessionId(response.getDetailedCookie("JSESSIONID"));
    }

    @When("perform get by browser")
    public void getByBrowser() {

        Response response = builderBrowser()
                .get(URL + "/{id}", "123");

        context.setResponse(response);
        authorizationContext.setLocation(response.getHeader("Location"));
        authorizationContext.setSessionId(response.getDetailedCookie("JSESSIONID"));
    }

    @When("perform head by application")
    public void headByApplication() {

        Response response = JsonRestTemplate.api()
                .queryParam("debug", "true")
                .head(URL + "/{id}", "123");

        context.setResponse(response);

    }

    @When("perform head by browser")
    public void headByBrowser() {

        Response response = builderBrowser()
                .head(URL + "/{id}", "123");

        context.setResponse(response);

    }

    @When("perform delete by application")
    public void deleteByApplication() {

        Response response = JsonRestTemplate.api()
                .queryParam("debug", "true")
                .delete(URL + "/{id}", "123");

        context.setResponse(response);
        authorizationContext.setLocation(response.getHeader("Location"));
        authorizationContext.setSessionId(response.getDetailedCookie("JSESSIONID"));
    }

    @When("perform delete by browser")
    public void deleteByBrowser() {

        Response response = builderBrowser()
                .delete(URL + "/{id}", "123");

        context.setResponse(response);
        authorizationContext.setLocation(response.getHeader("Location"));
        authorizationContext.setSessionId(response.getDetailedCookie("JSESSIONID"));
    }

    @When("perform patch by application")
    public void patchByApplication() {

        Map<String, Object> patch = Map.of(
                "op", "replace",
                "path", "/value",
                "value", UUID.randomUUID());

        Response response = JsonRestTemplate.api()
                .queryParam("debug", "true")
                .body(Collections.singletonList(patch)).patch(URL + "/{id}", "123");

        context.setResponse(response);
        authorizationContext.setLocation(response.getHeader("Location"));
        authorizationContext.setSessionId(response.getDetailedCookie("JSESSIONID"));
    }

    @When("perform patch by browser")
    public void patchByBrowser() {

        Map<String, Object> patch = Map.of(
                "op", "replace",
                "path", "/value",
                "value", UUID.randomUUID());

        Response response = builderBrowser()
                .body(Collections.singletonList(patch)).patch(URL + "/{id}", "123");

        context.setResponse(response);
        authorizationContext.setLocation(response.getHeader("Location"));
        authorizationContext.setSessionId(response.getDetailedCookie("JSESSIONID"));
    }

    @When("perform options by application")
    public void optionsByApplication() {

        Response response = JsonRestTemplate.api()
                .queryParam("debug", "true")
                .options(URL + "/{id}", "123");

        context.setResponse(response);

    }

    @When("perform options by browser")
    public void optionsByBrowser() {

        Response response = builderBrowser()
                .options(URL + "/{id}", "123");

        context.setResponse(response);

    }

    @Then("response status is {int}")
    public void checkResponse(int status) {

        assertThat(context.getResponse().getStatusCode()).isEqualTo(status);
    }

    private RequestSpecification builderBrowser() {

        return JsonRestTemplate.browser()
                .redirects().follow(false)
                .cookie("XSRF-TOKEN", authorizationContext.getXsrfToken().getValue())
                .cookie("JSESSIONID", authorizationContext.getSessionId().getValue())
                .header("X-XSRF-TOKEN", authorizationContext.getXsrfToken().getValue())
                .queryParam("debug", "true");
    }
}
