package com.exemple.gateway.core.session;

import java.text.ParseException;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.ReactiveOAuth2UserService;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.server.WebSessionServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
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
import com.nimbusds.jwt.SignedJWT;

import lombok.SneakyThrows;
import reactor.core.publisher.Mono;

@EnableHazelcastHttpSession
@Configuration
@EnableSpringWebSession
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
        return new HazelcastSessionRepository(sessionRepository);
    }

    @Bean
    public ServerOAuth2AuthorizedClientRepository authorizedClientRepository() {
        return new WebSessionServerOAuth2AuthorizedClientRepository();
    }

    @Bean
    @Primary
    public ReactiveOAuth2UserService<OAuth2UserRequest, OAuth2User> userService() {

        return new ReactiveOAuth2UserService<OAuth2UserRequest, OAuth2User>() {

            @Override
            @SneakyThrows(value = ParseException.class)
            public Mono<OAuth2User> loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

                var accessToken = userRequest.getAccessToken();
                var signedJwt = SignedJWT.parse(accessToken.getTokenValue());
                var subject = signedJwt.getJWTClaimsSet().getSubject();

                return Mono.just(new DefaultOAuth2User(accessToken.getScopes().stream().map(SimpleGrantedAuthority::new).toList(),
                        Map.of("subject", subject),
                        "subject"));
            }

        };
    }

    @Bean
    @Primary
    public WebSessionIdResolver webSessionIdResolver() {
        var resolver = new CookieWebSessionIdResolver();
        resolver.setCookieName("JSESSIONID");
        return resolver;
    }

}
