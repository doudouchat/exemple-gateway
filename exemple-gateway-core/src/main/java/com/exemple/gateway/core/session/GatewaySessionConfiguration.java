package com.exemple.gateway.core.session;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spring.session.HazelcastSessionConfiguration;
import com.hazelcast.spring.session.config.annotation.SpringSessionHazelcastInstance;
import com.hazelcast.spring.session.config.annotation.web.http.EnableHazelcastHttpSession;

@EnableHazelcastHttpSession
@Configuration
public class GatewaySessionConfiguration {

    @Bean
    @SpringSessionHazelcastInstance
    public HazelcastInstance hazelcastInstance() {
        var config = new Config();
        HazelcastSessionConfiguration.applySerializationConfig(config);
        return Hazelcast.newHazelcastInstance(config);
    }

}
