package com.exemple.gateway.integration.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import com.auth0.jwt.JWT;
import com.exemple.gateway.integration.common.JsonRestTemplate;
import com.exemple.gateway.integration.resource.TestAlgorithmConfiguration;

import io.restassured.response.Response;

class TestIT {

    private static final String URL = "/ws/test";

    private static String ACCESS_TOKEN;

    @BeforeAll
    public static void init() throws IOException, GeneralSecurityException {

        TestAlgorithmConfiguration algorithmConfiguration = new TestAlgorithmConfiguration(new ClassPathResource("public_key"),
                new ClassPathResource("private_key"));

        ACCESS_TOKEN = JWT.create().withAudience("test")
                .withArrayClaim("scope", new String[] { "test:read", "test:create", "test:delete", "test:update" })
                .sign(algorithmConfiguration.algo());

    }

    @Test
    void post() {

        // When perform post
        Map<String, Object> body = Map.of("value", UUID.randomUUID());

        Response response = JsonRestTemplate.api()
                .header("Authorization", "Bearer " + ACCESS_TOKEN).queryParam("debug", "true")
                .body(body).post(URL);

        // Then check response
        assertThat(response.getStatusCode()).isEqualTo(201);
    }

    @Test
    void head() {

        // When perform head
        Response response = JsonRestTemplate.api()
                .header("Authorization", "Bearer " + ACCESS_TOKEN).queryParam("debug", "true")
                .head(URL + "/{id}", "123");

        // Then check response
        assertThat(response.getStatusCode()).isEqualTo(204);

    }

    @Test
    void get() {

        // When perform get
        Response response = JsonRestTemplate.api()
                .header("Authorization", "Bearer " + ACCESS_TOKEN).queryParam("debug", "true")
                .get(URL + "/{id}", "123");

        // Then check response
        assertThat(response.getStatusCode()).isEqualTo(200);

    }

    @Test
    void delete() {

        // When perform delete
        Response response = JsonRestTemplate.api()
                .header("Authorization", "Bearer " + ACCESS_TOKEN).queryParam("debug", "true")
                .delete(URL + "/{id}", "123");

        // Then check response
        assertThat(response.getStatusCode()).isEqualTo(204);

    }

    @Test
    void patch() {

        // When perform patch
        Map<String, Object> patch = Map.of(
                "op", "replace",
                "path", "/value",
                "value", UUID.randomUUID());

        Response response = JsonRestTemplate.api()
                .header("Authorization", "Bearer " + ACCESS_TOKEN).queryParam("debug", "true")
                .body(Collections.singletonList(patch)).patch(URL + "/{id}", "123");

        // Then check response
        assertThat(response.getStatusCode()).isEqualTo(204);

    }

    @Test
    void options() {

        // When perform options
        Response response = JsonRestTemplate.api()
                .header("Authorization", "Bearer " + ACCESS_TOKEN).queryParam("debug", "true")
                .options(URL + "/{id}", "123");

        // Then check response
        assertThat(response.getStatusCode()).isEqualTo(200);

    }
}
