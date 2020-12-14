package com.exemple.gateway.session;

import org.springframework.context.annotation.Configuration;
import org.springframework.session.hazelcast.config.annotation.web.http.EnableHazelcastHttpSession;

@EnableHazelcastHttpSession
@Configuration
public class GatewaySessionConfiguration {

}
