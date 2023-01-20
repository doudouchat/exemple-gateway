package com.exemple.gateway.core.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class SetStatus200IfRedirectionGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    public SetStatus200IfRedirectionGatewayFilterFactory() {
        super(Object.class);
    }

    @Override
    public GatewayFilter apply(Object config) {

        return (ServerWebExchange exchange, GatewayFilterChain chain) -> chain.filter(exchange).then(Mono.fromRunnable(() -> {
            if (exchange.getResponse().getStatusCode().is3xxRedirection()) {
                exchange.getResponse().setStatusCode(HttpStatus.OK);
            }
        }));
    }
}
