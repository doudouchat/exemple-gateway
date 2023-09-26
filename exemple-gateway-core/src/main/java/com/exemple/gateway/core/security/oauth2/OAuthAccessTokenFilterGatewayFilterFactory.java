package com.exemple.gateway.core.security.oauth2;

import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyResponseBodyGatewayFilterFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.server.ServerWebExchange;

import com.exemple.gateway.core.security.helper.SessionHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;

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

                        cleanSessionCookie(exchange);

                        var session = saveSession(previousBody);

                        var newCookie = ResponseCookie.from("JSESSIONID", session.getId())
                                .secure(Optional.ofNullable(exchange.getRequest().getSslInfo()).isPresent())
                                .httpOnly(true)
                                .path("/")
                                .build();
                        exchange.getResponse().addCookie(newCookie);

                    }

                    return Mono.justOrEmpty(previousBody);
                });
    }

    @Override
    public GatewayFilter apply(Object config) {

        return gatewayFilterFactory.apply(this.config);
    }

    @SneakyThrows
    private Session saveSession(String body) {

        var session = repository.createSession();

        LOG.debug("session cookie is {}", session.getId());

        var tokenResponseBody = MAPPER.readTree(body);

        var accessToken = tokenResponseBody.path(ACCESS_TOKEN);
        if (!accessToken.isMissingNode()) {
            saveAccessToken(session, accessToken);
        }

        var refreshToken = tokenResponseBody.path(REFRESH_TOKEN);
        if (!refreshToken.isMissingNode()) {
            saveRefreshToken(session, refreshToken);
        }

        repository.save(session);

        return session;

    }

    private void cleanSessionCookie(ServerWebExchange exchange) {

        sessionHelper.extractAllSessionCookies(exchange.getRequest()).forEach((HttpCookie cookie) -> {
            repository.deleteById(cookie.getValue());
            exchange.getResponse().addCookie(ResponseCookie.fromClientResponse(cookie.getName(), null).maxAge(0).build());

        });
    }

    private void saveAccessToken(Session session, JsonNode accessToken) throws ParseException {

        session.setAttribute(ACCESS_TOKEN, accessToken.textValue());
        var expiresAt = SignedJWT.parse(accessToken.textValue()).getJWTClaimsSet().getExpirationTime();
        Assert.notNull(expiresAt, "exp is required");
        session.setMaxInactiveInterval(Duration.between(Instant.now(clock), expiresAt.toInstant()));
        LOG.debug("save access token in session {}", session.getId());
    }

    private void saveRefreshToken(Session session, JsonNode refreshToken) throws ParseException {

        session.setAttribute(REFRESH_TOKEN, refreshToken.textValue());
        LOG.debug("save refresh token in session {}", session.getId());

    }
}
