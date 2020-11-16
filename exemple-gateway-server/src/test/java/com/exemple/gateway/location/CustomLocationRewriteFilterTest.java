package com.exemple.gateway.location;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.exemple.gateway.common.LoggingFilter;
import com.exemple.gateway.core.GatewayServerTestConfiguration;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

public class CustomLocationRewriteFilterTest extends GatewayServerTestConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(CustomLocationRewriteFilterTest.class);

    @Autowired
    private TestRestTemplate restTemplate;

    private RequestSpecification requestSpecification;

    @BeforeMethod
    private void before() {

        requestSpecification = RestAssured.given().filters(new LoggingFilter(LOG));

        apiClient.reset();
        authorizationClient.reset();

    }

    @Test
    public void location201Rewrite() {

        apiClient.when(HttpRequest.request().withMethod("GET").withPath("/ExempleService/info"))
                .respond(
                        HttpResponse.response()
                                .withHeaders(new Header("Content-Type", "application/json;charset=UTF-8"),
                                        new Header("Location", "http://localhost:" + this.apiClient.getPort() + "/ExempleService/123"))
                                .withStatusCode(201));

        Response response = requestSpecification.get(restTemplate.getRootUri() + "/ExempleService/info");

        assertThat(response.getStatusCode(), is(HttpStatus.CREATED.value()));
        assertThat(response.getHeader("Location"), is(restTemplate.getRootUri() + "/ExempleService/123"));

    }

    @Test
    public void location302Rewrite() {

        authorizationClient.when(HttpRequest.request().withMethod("POST").withPath("/ExempleAuthorization/oauth/info"))
                .respond(HttpResponse.response()
                        .withHeaders(new Header("Content-Type", "application/json;charset=UTF-8"),
                                new Header("Location", "http://localhost:" + this.authorizationClient.getPort() + "/ExempleAuthorization/123"))
                        .withStatusCode(302));
        Response response = requestSpecification.post(restTemplate.getRootUri() + "/ExempleAuthorization/oauth/info");

        assertThat(response.getStatusCode(), is(HttpStatus.FOUND.value()));
        assertThat(response.getHeader("Location"), is(restTemplate.getRootUri() + "/ExempleAuthorization/123"));

    }

}
