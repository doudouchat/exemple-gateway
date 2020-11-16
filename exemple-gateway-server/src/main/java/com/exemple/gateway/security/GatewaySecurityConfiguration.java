package com.exemple.gateway.security;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity.CsrfSpec;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.security.web.server.csrf.CsrfWebFilter;
import org.springframework.security.web.server.util.matcher.AndServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.OrServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;

@Configuration
@ComponentScan(basePackages = "com.exemple.gateway.security")
public class GatewaySecurityConfiguration {

    private final String[] excludes;

    private final ServerAuthenticationConverter tokenExtractor;

    public GatewaySecurityConfiguration(ServerAuthenticationConverter tokenExtractor, @Value("${gateway.security.excludes:}") String[] excludes) {

        this.tokenExtractor = tokenExtractor;
        this.excludes = excludes.clone();
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http, Customizer<CsrfSpec> csrfCustomizer) {

        return http

                .cors().and()

                .authorizeExchange().pathMatchers(this.excludes).permitAll()

                .anyExchange().authenticated().and()

                .csrf(csrfCustomizer)

                .oauth2ResourceServer().jwt().and().bearerTokenConverter(tokenExtractor).and()

                .build();
    }

    @Profile("browser")
    public static class GatewaySecurityConfigurationCsrf {

        private final ServerWebExchangeMatcher requireCsrfProtectionMatcher;

        public GatewaySecurityConfigurationCsrf(@Value("${gateway.security.csrf.excludes:**}") String[] excludesCsrf) {

            requireCsrfProtectionMatcher = new AndServerWebExchangeMatcher(CsrfWebFilter.DEFAULT_CSRF_MATCHER,
                    new NegatedServerWebExchangeMatcher(new OrServerWebExchangeMatcher(
                            Arrays.stream(excludesCsrf).map(PathPatternParserServerWebExchangeMatcher::new).collect(Collectors.toList()))));
        }

        @Bean
        public Customizer<CsrfSpec> csrf() {

            return (CsrfSpec csrf) -> csrf.csrfTokenRepository(CookieServerCsrfTokenRepository.withHttpOnlyFalse())
                    .requireCsrfProtectionMatcher(requireCsrfProtectionMatcher);
        }
    }

    @Profile("!browser")
    public static class GatewaySecurityConfigurationDisableCsrf {

        @Bean
        public Customizer<CsrfSpec> csrf() {

            return CsrfSpec::disable;
        }
    }
}
