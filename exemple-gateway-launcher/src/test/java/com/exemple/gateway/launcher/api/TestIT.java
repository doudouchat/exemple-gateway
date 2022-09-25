package com.exemple.gateway.launcher.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.exemple.gateway.integration.resource.TestAlgorithmConfiguration;
import com.exemple.gateway.launcher.common.JsonRestTemplate;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import io.restassured.response.Response;

@SpringJUnitConfig(TestAlgorithmConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestIT {

    private static final String URL = "/ws/test";

    private static SignedJWT ACCESS_TOKEN;

    @Autowired
    private JWSSigner signer;

    @BeforeAll
    public void init() throws JOSEException {

        var payload = new JWTClaimsSet.Builder()
                .audience("test")
                .claim("scope", new String[] { "test:read", "test:create", "test:delete", "test:update" })
                .build();

        ACCESS_TOKEN = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).build(), payload);
        ACCESS_TOKEN.sign(signer);

    }

    @Test
    void post() {

        // When perform post
        Map<String, Object> body = Map.of("value", UUID.randomUUID());

        Response response = JsonRestTemplate.api()
                .header("Authorization", "Bearer " + ACCESS_TOKEN.serialize()).queryParam("debug", "true")
                .body(body).post(URL);

        // Then check response
        assertThat(response.getStatusCode()).isEqualTo(201);
    }

    @Test
    void head() {

        // When perform head
        Response response = JsonRestTemplate.api()
                .header("Authorization", "Bearer " + ACCESS_TOKEN.serialize()).queryParam("debug", "true")
                .head(URL + "/{id}", "123");

        // Then check response
        assertThat(response.getStatusCode()).isEqualTo(204);

    }

    @Test
    void get() {

        // When perform get
        Response response = JsonRestTemplate.api()
                .header("Authorization", "Bearer " + ACCESS_TOKEN.serialize()).queryParam("debug", "true")
                .get(URL + "/{id}", "123");

        // Then check response
        assertThat(response.getStatusCode()).isEqualTo(200);

    }

    @Test
    void delete() {

        // When perform delete
        Response response = JsonRestTemplate.api()
                .header("Authorization", "Bearer " + ACCESS_TOKEN.serialize()).queryParam("debug", "true")
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
                .header("Authorization", "Bearer " + ACCESS_TOKEN.serialize()).queryParam("debug", "true")
                .body(Collections.singletonList(patch)).patch(URL + "/{id}", "123");

        // Then check response
        assertThat(response.getStatusCode()).isEqualTo(204);

    }

    @Test
    void options() {

        // When perform options
        Response response = JsonRestTemplate.api()
                .header("Authorization", "Bearer " + ACCESS_TOKEN.serialize()).queryParam("debug", "true")
                .options(URL + "/{id}", "123");

        // Then check response
        assertThat(response.getStatusCode()).isEqualTo(200);

    }
}
