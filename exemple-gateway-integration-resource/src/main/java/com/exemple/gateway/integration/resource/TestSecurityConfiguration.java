package com.exemple.gateway.integration.resource;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.MapSession;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;
import org.springframework.session.security.SpringSessionBackedSessionRegistry;
import org.springframework.session.web.http.HeaderHttpSessionIdResolver;
import org.springframework.session.web.http.HttpSessionIdResolver;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableSpringHttpSession
public class TestSecurityConfiguration {

    @Bean
    public InMemoryUserDetailsManager userDetailsService() {

        String password = "{bcrypt}" + BCrypt.hashpw("123", BCrypt.gensalt());

        var manager = new InMemoryUserDetailsManager();
        manager.createUser(User.withUsername("jean.dupond@gmail.com").password(password).authorities("USER").build());

        return manager;
    }

    @Bean
    public HttpSessionIdResolver httpSessionIdResolver() {
        return HeaderHttpSessionIdResolver.xAuthToken();
    }

    @Bean
    public MapSessionRepository sessionRepository() {
        return new MapSessionRepository(new ConcurrentHashMap<>());
    }

    @Bean
    public AuthenticationManager authenticationManager() {

        AbstractUserDetailsAuthenticationProvider provider = new AbstractUserDetailsAuthenticationProvider() {

            @Override
            protected void additionalAuthenticationChecks(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) {
                // NOP
            }

            @Override
            protected UserDetails retrieveUser(String username, UsernamePasswordAuthenticationToken authentication) {
                return userDetailsService().loadUserByUsername(username);
            }
        };

        return new ProviderManager(provider);

    }

    @Bean
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {

        var sessionStrategy = new RegisterSessionAuthenticationStrategy(
                new SpringSessionBackedSessionRegistry<>(new MapSessionRegistry(sessionRepository())));

        var filter = new UsernamePasswordAuthenticationFilter();
        filter.setAuthenticationManager(authenticationManager());
        filter.setSessionAuthenticationStrategy(sessionStrategy);
        filter.setSecurityContextRepository(new HttpSessionSecurityContextRepository());

        http
                .addFilter(filter)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2ResourceServer -> oauth2ResourceServer.jwt(Customizer.withDefaults()))
                .csrf(csrf -> csrf.ignoringRequestMatchers(new AntPathRequestMatcher("/login")));

        return http.build();

    }

    @RequiredArgsConstructor
    private static class MapSessionRegistry implements FindByIndexNameSessionRepository<MapSession> {

        private final MapSessionRepository sessionRepository;

        @Override
        public MapSession createSession() {
            return sessionRepository.createSession();
        }

        @Override
        public void save(MapSession session) {
            sessionRepository.save(session);
        }

        @Override
        public MapSession findById(String id) {
            return sessionRepository.findById(id);
        }

        @Override
        public void deleteById(String id) {
            sessionRepository.deleteById(id);
        }

        @Override
        public Map<String, MapSession> findByIndexNameAndIndexValue(String indexName, String indexValue) {
            return Map.of();
        }

    }

}
