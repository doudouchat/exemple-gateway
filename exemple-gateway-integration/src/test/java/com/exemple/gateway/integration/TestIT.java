package com.exemple.gateway.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.testng.annotations.Test;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.exemple.service.api.integration.core.JsonRestTemplate;

import io.restassured.response.Response;

public class TestIT {

    private static final String URL = "/ws/test";

    private static final String ACCESS_TOKEN;

    private static final Algorithm HMAC256_ALGORITHM;

    static {

        HMAC256_ALGORITHM = Algorithm.HMAC256("abc");

        ACCESS_TOKEN = JWT.create().sign(HMAC256_ALGORITHM);

    }

    @Test
    public void post() {

        Map<String, Object> body = new HashMap<>();
        body.put("value", UUID.randomUUID());

        Response response = JsonRestTemplate.given()

                .header("Authorization", "Bearer " + ACCESS_TOKEN).queryParam("debug", "true")

                .body(body).post(URL);

        assertThat(response.getStatusCode(), is(HttpStatus.CREATED.value()));

    }

    @Test
    public void head() {

        Response response = JsonRestTemplate.given()

                .header("Authorization", "Bearer " + ACCESS_TOKEN).queryParam("debug", "true")

                .head(URL + "/{id}", "123");

        assertThat(response.getStatusCode(), is(HttpStatus.NO_CONTENT.value()));

    }

    @Test
    public void get() {

        Response response = JsonRestTemplate.given()

                .header("Authorization", "Bearer " + ACCESS_TOKEN).queryParam("debug", "true")

                .get(URL + "/{id}", "123");

        assertThat(response.getStatusCode(), is(HttpStatus.OK.value()));

    }

    @Test
    public void delete() {

        Response response = JsonRestTemplate.given()

                .header("Authorization", "Bearer " + ACCESS_TOKEN).queryParam("debug", "true")

                .delete(URL + "/{id}", "123");

        assertThat(response.getStatusCode(), is(HttpStatus.NO_CONTENT.value()));

    }

    @Test
    public void patch() {

        Map<String, Object> patch = new HashMap<>();
        patch.put("op", "replace");
        patch.put("path", "/value");
        patch.put("value", UUID.randomUUID());

        Response response = JsonRestTemplate.given()

                .header("Authorization", "Bearer " + ACCESS_TOKEN).queryParam("debug", "true")

                .body(Collections.singletonList(patch)).patch(URL + "/{id}", "123");

        assertThat(response.getStatusCode(), is(HttpStatus.NO_CONTENT.value()));

    }

    @Test
    public void options() {

        Response response = JsonRestTemplate.given()

                .header("Authorization", "Bearer " + ACCESS_TOKEN).queryParam("debug", "true")

                .options(URL + "/{id}", "123");

        assertThat(response.getStatusCode(), is(HttpStatus.OK.value()));

    }
}
