package com.exemple.gateway.core;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;

import com.exemple.gateway.core.security.GatewaySecurityConfiguration;
import com.exemple.gateway.core.session.GatewaySessionConfiguration;

@Configuration
@EnableWebFlux
@Import({ GatewaySecurityConfiguration.class, GatewaySessionConfiguration.class })
public class GatewayConfiguration implements WebFluxConfigurer {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Override
    public void addCorsMappings(CorsRegistry corsRegistry) {
        corsRegistry.addMapping("/**").allowedOrigins("*").allowedHeaders("*").allowedMethods("GET", "POST", "PUT", "HEAD", "PATCH")
                .allowCredentials(true);
    }
}
