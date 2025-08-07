package com.exemple.gateway.launcher;

import java.util.UUID;

import org.springframework.test.context.ContextConfiguration;

import io.cucumber.java.DocStringType;
import io.cucumber.java.ParameterType;
import io.cucumber.spring.CucumberContextConfiguration;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@CucumberContextConfiguration
@ContextConfiguration(classes = GatewayTestConfiguration.class)
public class CucumberConfiguration {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @DocStringType
    public JsonNode json(String content) {
        return MAPPER.readTree(content);
    }

    @ParameterType(".*")
    public UUID id(String id) {
        return UUID.fromString(id);
    }

}
