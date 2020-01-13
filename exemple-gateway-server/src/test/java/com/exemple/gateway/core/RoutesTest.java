package com.exemple.gateway.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Collections;

import org.mockserver.client.MockServerClient;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.JsonBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.exemple.gateway.common.LoggingFilter;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

@SpringBootTest(classes = { GatewayTestConfiguration.class, GatewayServerTestConfiguration.class }, webEnvironment = WebEnvironment.RANDOM_PORT)
public class RoutesTest extends AbstractTestNGSpringContextTests {

    private static final Logger LOG = LoggerFactory.getLogger(RoutesTest.class);

    @Autowired
    private TestRestTemplate restTemplate;

    @Value("${api.port}")
    private int apiPort;

    @Value("${authorization.port}")
    private int authorizationPort;

    private RequestSpecification requestSpecification;

    @Autowired
    private MockServerClient apiClient;

    @Autowired
    private MockServerClient authorizationClient;

    @BeforeMethod
    private void before() {

        requestSpecification = RestAssured.given().filters(new LoggingFilter(LOG));

        apiClient.reset();
        authorizationClient.reset();

    }

    @Test
    public void api() {

        apiClient.when(HttpRequest.request().withMethod("GET").withPath("/ExempleService/info"))
                .respond(HttpResponse.response().withHeaders(new Header("Content-Type", "application/json;charset=UTF-8"))
                        .withBody(JsonBody.json(Collections.singletonMap("name", "jean"))).withStatusCode(200));

        Response response = requestSpecification.get(restTemplate.getRootUri() + "/ExempleService/info");

        assertThat(response.getStatusCode(), is(HttpStatus.OK.value()));
        assertThat(response.jsonPath().get("name"), is("jean"));

    }

    @Test
    public void authorization() {

        authorizationClient.when(HttpRequest.request().withMethod("GET").withPath("/ExempleAuthorization/info"))
                .respond(HttpResponse.response().withStatusCode(200));

        Response response = requestSpecification.get(restTemplate.getRootUri() + "/ExempleAuthorization/info");

        assertThat(response.getStatusCode(), is(HttpStatus.OK.value()));

    }

}
