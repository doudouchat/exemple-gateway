package com.exemple.gateway.launcher;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Import;

import com.exemple.gateway.core.GatewayConfiguration;

@SpringBootApplication
@Import(GatewayConfiguration.class)
public class GatewayServerApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(GatewayServerApplication.class).run(args);
    }

}
