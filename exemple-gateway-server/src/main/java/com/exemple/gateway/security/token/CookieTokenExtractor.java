package com.exemple.gateway.security.token;

import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.server.ServerBearerTokenAuthenticationConverter;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.exemple.gateway.security.helper.SessionHelper;

import reactor.core.publisher.Mono;

@Component
public class CookieTokenExtractor implements ServerAuthenticationConverter {

    private final ServerAuthenticationConverter defaultTokenExtractor;

    private final SessionHelper sessionHelper;

    public CookieTokenExtractor(SessionHelper sessionHelper) {

        this.defaultTokenExtractor = new ServerBearerTokenAuthenticationConverter();
        this.sessionHelper = sessionHelper;
    }

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {

        Supplier<Mono<Authentication>> extractCookie = () -> Mono.justOrEmpty(sessionHelper.extractSessionCookie(exchange.getRequest())
                .map(Pair::getValue).map(session -> new BearerTokenAuthenticationToken(session.getAttribute("access_token"))));

        return this.defaultTokenExtractor.convert(exchange).switchIfEmpty(Mono.defer(extractCookie));
    }

}
