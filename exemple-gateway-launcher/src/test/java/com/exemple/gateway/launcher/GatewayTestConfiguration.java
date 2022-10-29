package com.exemple.gateway.launcher;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.exemple.gateway.integration.resource.TestAlgorithmConfiguration;

@Configuration
@Import(TestAlgorithmConfiguration.class)
@ComponentScan(basePackages = { "com.exemple.gateway.launcher.api", "com.exemple.gateway.launcher.token" })
public class GatewayTestConfiguration {

}
