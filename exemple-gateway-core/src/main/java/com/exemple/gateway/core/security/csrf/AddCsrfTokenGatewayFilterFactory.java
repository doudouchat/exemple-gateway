package com.exemple.gateway.core.security.csrf;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class AddCsrfTokenGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    private final CookieServerCsrfTokenRepository csrfTokenRepository;

    public AddCsrfTokenGatewayFilterFactory() {
        super(Object.class);
        this.csrfTokenRepository = CookieServerCsrfTokenRepository.withHttpOnlyFalse();
    }

    @Override
    public GatewayFilter apply(Object config) {

        return (exchange, chain) -> Mono.defer(() -> csrfToken(exchange)).then(chain.filter(exchange));
    }

    private Mono<CsrfToken> csrfToken(ServerWebExchange exchange) {
        return this.csrfTokenRepository.loadToken(exchange).switchIfEmpty(generateToken(exchange));
    }

    private Mono<CsrfToken> generateToken(ServerWebExchange exchange) {
        return this.csrfTokenRepository.generateToken(exchange).delayUntil(token -> this.csrfTokenRepository.saveToken(exchange, token));
    }

}
