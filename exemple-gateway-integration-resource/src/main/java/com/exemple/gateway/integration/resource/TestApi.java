package com.exemple.gateway.integration.resource;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping(value = "/ws/test")
@Slf4j
public class TestApi {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequestMapping(value = "/{id}", method = RequestMethod.HEAD)
    public void head(@PathVariable("id") String id) {

        LOG.info("head api");

    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public void post(@RequestBody JsonNode source) {

        LOG.info("create api");

    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping(value = "/{id}")
    public void update(@PathVariable("id") String id, @RequestBody ArrayNode patch) {

        LOG.info("patch api");

    }

    @GetMapping(value = "/{id}")
    public JsonNode get(@PathVariable("id") String id) {

        LOG.info("get api");

        return MAPPER.createObjectNode();

    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping(value = "/{id}")
    public void delete(@PathVariable("id") String id) {

        LOG.info("delete api");

    }
}
