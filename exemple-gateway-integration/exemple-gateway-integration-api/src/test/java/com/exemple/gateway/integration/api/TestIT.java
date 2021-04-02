package com.exemple.gateway.integration.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.core.io.ClassPathResource;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.auth0.jwt.JWT;
import com.exemple.gateway.integration.common.JsonRestTemplate;
import com.exemple.gateway.integration.resource.TestAlgorithmConfiguration;

import io.restassured.response.Response;

public class TestIT {

    private static final String URL = "/ws/test";

    private String ACCESS_TOKEN;

    @BeforeClass
    public void init() throws IOException, GeneralSecurityException {

        TestAlgorithmConfiguration algorithmConfiguration = new TestAlgorithmConfiguration(new ClassPathResource("public_key"),
                new ClassPathResource("private_key"));

        ACCESS_TOKEN = JWT.create().withAudience("test")
                .withArrayClaim("scope", new String[] { "test:read", "test:create", "test:delete", "test:update" })
                .sign(algorithmConfiguration.algo());

    }

    @Test
    public void post() {

        Map<String, Object> body = new HashMap<>();
        body.put("value", UUID.randomUUID());

        Response response = JsonRestTemplate.given()

                .header("Authorization", "Bearer " + ACCESS_TOKEN).queryParam("debug", "true")

                .body(body).post(URL);

        assertThat(response.getStatusCode(), is(201));

    }

    @Test
    public void head() {

        Response response = JsonRestTemplate.given()

                .header("Authorization", "Bearer " + ACCESS_TOKEN).queryParam("debug", "true")

                .head(URL + "/{id}", "123");

        assertThat(response.getStatusCode(), is(204));

    }

    @Test
    public void get() {

        Response response = JsonRestTemplate.given()

                .header("Authorization", "Bearer " + ACCESS_TOKEN).queryParam("debug", "true")

                .get(URL + "/{id}", "123");

        assertThat(response.getStatusCode(), is(200));

    }

    @Test
    public void delete() {

        Response response = JsonRestTemplate.given()

                .header("Authorization", "Bearer " + ACCESS_TOKEN).queryParam("debug", "true")

                .delete(URL + "/{id}", "123");

        assertThat(response.getStatusCode(), is(204));

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

        assertThat(response.getStatusCode(), is(204));

    }

    @Test
    public void options() {

        Response response = JsonRestTemplate.given()

                .header("Authorization", "Bearer " + ACCESS_TOKEN).queryParam("debug", "true")

                .options(URL + "/{id}", "123");

        assertThat(response.getStatusCode(), is(200));

    }
}
