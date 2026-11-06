package com.exemple.gateway.core.session;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.session.MapSession;
import org.springframework.session.ReactiveMapSessionRepository;
import org.springframework.session.ReactiveSessionRepository;
import org.springframework.session.config.annotation.web.server.EnableSpringWebSession;
import org.springframework.web.server.session.CookieWebSessionIdResolver;
import org.springframework.web.server.session.WebSessionIdResolver;

import com.hazelcast.config.Config;
import com.hazelcast.config.IndexConfig;
import com.hazelcast.config.IndexType;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spring.session.HazelcastIndexedSessionRepository;
import com.hazelcast.spring.session.HazelcastIndexedSessionRepository.HazelcastSession;
import com.hazelcast.spring.session.HazelcastSessionConfiguration;
import com.hazelcast.spring.session.config.annotation.SpringSessionHazelcastInstance;
import com.hazelcast.spring.session.config.annotation.web.http.EnableHazelcastHttpSession;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@EnableHazelcastHttpSession
@Configuration
@EnableSpringWebSession
@Slf4j
public class GatewaySessionConfiguration {

    @Bean
    @SpringSessionHazelcastInstance
    public HazelcastInstance hazelcastInstance() {
        var config = new Config();
        HazelcastSessionConfiguration.applySerializationConfig(config);
        config.getMapConfig(HazelcastIndexedSessionRepository.DEFAULT_SESSION_MAP_NAME)
                .addIndexConfig(
                        new IndexConfig(IndexType.HASH, HazelcastIndexedSessionRepository.PRINCIPAL_NAME_ATTRIBUTE));
        return Hazelcast.newHazelcastInstance(config);
    }

    @Bean
    public ReactiveSessionRepository<MapSession> reactiveSessionRepository(HazelcastInstance hazelcastInstance) {
        return new ReactiveMapSessionRepository(hazelcastInstance.getMap(HazelcastIndexedSessionRepository.DEFAULT_SESSION_MAP_NAME));
    }

    @Bean
    @Primary
    public ReactiveSessionRepository<HazelcastSession> hazelcastSessionRepository(HazelcastIndexedSessionRepository sessionRepository) {
        return new ReactiveSessionRepository<HazelcastSession>() {

            @Override
            public Mono<HazelcastSession> createSession() {
                return Mono.just(sessionRepository.createSession());
            }

            @Override
            public Mono<Void> save(HazelcastSession session) {
                sessionRepository.save(session);
                LOG.debug("Session saved: {}", session.getId());
                return Mono.empty();
            }

            @Override
            public Mono<HazelcastSession> findById(String id) {
                return Mono.justOrEmpty(sessionRepository.findById(id));
            }

            @Override
            public Mono<Void> deleteById(String id) {
                sessionRepository.deleteById(id);
                return Mono.empty();
            }

        };
    }

    @Bean
    @Primary
    public WebSessionIdResolver webSessionIdResolver() {
        var resolver = new CookieWebSessionIdResolver();
        resolver.setCookieName("JSESSIONID");
        resolver.addCookieInitializer(builder -> builder.path("/"));
        resolver.addCookieInitializer(builder -> builder.sameSite("Strict"));
        return resolver;
    }

}
