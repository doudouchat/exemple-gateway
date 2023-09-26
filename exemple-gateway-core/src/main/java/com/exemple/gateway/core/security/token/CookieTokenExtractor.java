package com.exemple.gateway.core.security.token;

import java.util.function.Supplier;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.server.authentication.ServerBearerTokenAuthenticationConverter;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.exemple.gateway.core.security.helper.SessionHelper;
import com.exemple.gateway.core.security.helper.SessionHelper.SessionCookie;

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
                .map(SessionCookie::session)
                .map(session -> new BearerTokenAuthenticationToken(session.getAttribute("access_token"))));

        return this.defaultTokenExtractor.convert(exchange).switchIfEmpty(Mono.defer(extractCookie));
    }

}
