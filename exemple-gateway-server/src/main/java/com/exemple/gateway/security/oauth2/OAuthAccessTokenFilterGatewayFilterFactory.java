package com.exemple.gateway.security.oauth2;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyResponseBodyGatewayFilterFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseCookie.ResponseCookieBuilder;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.server.ServerWebExchange;

import com.auth0.jwt.JWT;
import com.exemple.gateway.security.helper.SessionHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class OAuthAccessTokenFilterGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String ACCESS_TOKEN = "access_token";

    private static final String REFRESH_TOKEN = "refresh_token";

    private final SessionRepository<Session> repository;

    private final SessionHelper sessionHelper;

    private final ModifyResponseBodyGatewayFilterFactory gatewayFilterFactory;

    private final ModifyResponseBodyGatewayFilterFactory.Config config;

    private final Clock clock;

    public OAuthAccessTokenFilterGatewayFilterFactory(SessionRepository<Session> repository, SessionHelper sessionHelper, Clock clock) {
        super(Object.class);
        this.repository = repository;
        this.sessionHelper = sessionHelper;
        this.clock = clock;
        this.gatewayFilterFactory = new ModifyResponseBodyGatewayFilterFactory(HandlerStrategies.withDefaults().messageReaders(),
                Collections.emptySet(), Collections.emptySet());
        this.config = new ModifyResponseBodyGatewayFilterFactory.Config().setRewriteFunction(String.class, String.class,
                (ServerWebExchange exchange, String previousBody) -> {

                    if (exchange.getResponse().getStatusCode().is2xxSuccessful()) {
                        saveSession(exchange, previousBody);
                    }

                    return Mono.justOrEmpty(previousBody);
                });
    }

    @Override
    public GatewayFilter apply(Object config) {

        return gatewayFilterFactory.apply(this.config);
    }

    @SneakyThrows
    private void saveSession(ServerWebExchange exchange, String body) {

        cleanSessionCookie(exchange);

        Pair<ResponseCookieBuilder, Session> cookieAndSession = extractSessionCookie(exchange);

        var session = cookieAndSession.getRight();
        ResponseCookieBuilder sessionCookie = cookieAndSession.getLeft();

        LOG.debug("session cookie is {}", session.getId());

        saveTokens(session, MAPPER.readTree(body));
        exchange.getResponse().addCookie(sessionCookie.build());

    }

    private Pair<ResponseCookieBuilder, Session> extractSessionCookie(ServerWebExchange exchange) {

        return sessionHelper.extractSessionCookie(exchange.getRequest())
                .map(p -> Pair.of(ResponseCookie.from(p.getKey().getName(), p.getKey().getValue()), p.getValue())).orElseGet(() -> {
                    var session = repository.createSession();
                    return Pair.of(createSessionCookie(exchange.getRequest(), session.getId()), session);
                });
    }

    private static ResponseCookieBuilder createSessionCookie(ServerHttpRequest request, String sessionId) {

        LOG.debug("new session cookie {}", sessionId);

        return ResponseCookie.from("JSESSIONID", sessionId).secure(Optional.ofNullable(request.getSslInfo()).isPresent()).httpOnly(true).path("/");
    }

    private void cleanSessionCookie(ServerWebExchange exchange) {

        sessionHelper.extractAllSessionCookies(exchange.getRequest()).forEach((HttpCookie cookie) -> {
            repository.deleteById(cookie.getValue());
            exchange.getResponse().addCookie(ResponseCookie.fromClientResponse(cookie.getName(), null).maxAge(0).build());

        });
    }

    private void saveTokens(Session session, JsonNode authorizationNode) {

        if (!authorizationNode.path(ACCESS_TOKEN).isMissingNode()) {
            session.setAttribute(ACCESS_TOKEN, authorizationNode.get(ACCESS_TOKEN).textValue());
            Date expiresAt = JWT.decode(authorizationNode.get(ACCESS_TOKEN).textValue()).getExpiresAt();
            Assert.notNull(expiresAt, "exp is required");
            session.setMaxInactiveInterval(Duration.between(Instant.now(clock), expiresAt.toInstant()));
            LOG.debug("save access token in session {}", session.getId());

        }

        if (!authorizationNode.path(REFRESH_TOKEN).isMissingNode()) {
            session.setAttribute(REFRESH_TOKEN, authorizationNode.get(REFRESH_TOKEN).textValue());
            LOG.debug("save refresh token in session {}", session.getId());

        }

        repository.save(session);

    }
}
