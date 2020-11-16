package com.exemple.gateway.core;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class GatewayServerApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(GatewayServerApplication.class).run(args);
    }

}
