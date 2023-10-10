package com.exemple.gateway.core.security.oauth2;

import java.util.Optional;
import java.util.function.Consumer;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyRequestBodyGatewayFilterFactory;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.server.ServerWebExchange;

import com.exemple.gateway.core.security.helper.SessionHelper;

import reactor.core.publisher.Mono;

@Component
public class OAuthRevokeTokenGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    private static final String ACCESS_TOKEN = "access_token";

    private final ModifyRequestBodyGatewayFilterFactory modifyRequestFactory;

    private final ModifyRequestBodyGatewayFilterFactory.Config modifyRequestConfig;

    private final SessionHelper sessionHelper;

    private final SessionRepository<Session> repository;

    public OAuthRevokeTokenGatewayFilterFactory(SessionRepository<Session> repository, SessionHelper sessionHelper) {
        super(Object.class);
        this.sessionHelper = sessionHelper;
        this.repository = repository;
        this.modifyRequestFactory = new ModifyRequestBodyGatewayFilterFactory(HandlerStrategies.withDefaults().messageReaders());
        this.modifyRequestConfig = new ModifyRequestBodyGatewayFilterFactory.Config()
                .setRewriteFunction(
                        String.class,
                        String.class,
                        (ServerWebExchange exchange, String nextBody) -> Mono.justOrEmpty(addTokenInForm(exchange, nextBody)));
    }

    @Override
    public GatewayFilter apply(Object config) {

        Consumer<ServerWebExchange> deleteSession = (ServerWebExchange exchange) -> {
            if (exchange.getResponse().getStatusCode().is2xxSuccessful()) {

                sessionHelper.extractSessionCookie(exchange.getRequest())
                        .ifPresent(sessionCookie -> repository.deleteById(sessionCookie.session().getId()));

            }
        };

        return (ServerWebExchange exchange, GatewayFilterChain chain) -> modifyRequestFactory.apply(modifyRequestConfig).filter(exchange, chain)
                .doOnSuccess((Void v) -> deleteSession.accept(exchange));

    }

    private String addTokenInForm(ServerWebExchange exchange, String defaultBody) {

        return sessionHelper.extractSessionCookie(exchange.getRequest())
                .flatMap(sessionCookie -> Optional.ofNullable("token=" + sessionCookie.session().getAttribute(ACCESS_TOKEN)))
                .orElse(defaultBody);
    }
}
