package com.exemple.gateway.core.security.token;

import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.server.ServerBearerTokenAuthenticationConverter;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.exemple.gateway.core.security.helper.SessionHelper;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class CookieTokenExtractor implements ServerAuthenticationConverter {

    private final ServerAuthenticationConverter defaultTokenExtractor = new ServerBearerTokenAuthenticationConverter();

    private final SessionHelper sessionHelper;

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {

        Supplier<Mono<Authentication>> extractCookie = () -> Mono.justOrEmpty(sessionHelper.extractSessionCookie(exchange.getRequest())
                .map(Pair::getValue).map(session -> new BearerTokenAuthenticationToken(session.getAttribute("access_token"))));

        return this.defaultTokenExtractor.convert(exchange).switchIfEmpty(Mono.defer(extractCookie));
    }

}
