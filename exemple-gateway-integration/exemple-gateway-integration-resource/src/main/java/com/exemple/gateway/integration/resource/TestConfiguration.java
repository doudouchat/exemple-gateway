package com.exemple.gateway.integration.resource;

import java.util.logging.Level;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJsonProvider;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.context.annotation.Configuration;

@Configuration
@ApplicationPath("/ws")
public class TestConfiguration extends ResourceConfig {

    public TestConfiguration() {

        // Test API
        super.register(TestApi.class)

                // Nom de l'application
                .setApplicationName("WS Test")

                // logging

                .register(LoggingFeature.class)

                .property(LoggingFeature.LOGGING_FEATURE_VERBOSITY, LoggingFeature.Verbosity.PAYLOAD_ANY)

                .property(LoggingFeature.LOGGING_FEATURE_LOGGER_LEVEL, Level.FINE.getName())

                // JSON
                .register(JacksonJsonProvider.class);

    }

}
