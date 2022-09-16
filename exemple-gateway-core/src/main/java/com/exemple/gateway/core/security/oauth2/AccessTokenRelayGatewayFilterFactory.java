package com.exemple.gateway.core.security.oauth2;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class AccessTokenRelayGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> extractJwtAuthenticationToken(exchange).map(token -> withBearerAuth(exchange, token))

                .defaultIfEmpty(exchange).flatMap(chain::filter);
    }

    private static Mono<JwtAuthenticationToken> extractJwtAuthenticationToken(ServerWebExchange exchange) {
        return exchange.getPrincipal().filter(JwtAuthenticationToken.class::isInstance).cast(JwtAuthenticationToken.class);
    }

    private static ServerWebExchange withBearerAuth(ServerWebExchange exchange, JwtAuthenticationToken accessToken) {
        return exchange.mutate().request(r -> r.headers(headers -> headers.setBearerAuth(accessToken.getToken().getTokenValue()))).build();
    }

}
