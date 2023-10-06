package com.exemple.gateway.core.security.oauth2;

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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class OAuthAccessTokenFilterGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

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

    private Session saveSession(String body) {

        var session = repository.createSession();

        LOG.debug("session cookie is {}", session.getId());

        var accessTokenResponse = new AccessTokenResponse(body);

        saveAccessToken(session, accessTokenResponse.accessToken);

        if (accessTokenResponse.hasRefreshToken()) {
            saveRefreshToken(session, accessTokenResponse.refreshToken);
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

    @SneakyThrows
    private void saveAccessToken(Session session, SignedJWT accessToken) {

        session.setAttribute(ACCESS_TOKEN, accessToken.serialize());
        var expiresAt = accessToken.getJWTClaimsSet().getExpirationTime();
        Assert.notNull(expiresAt, "exp is required");
        session.setMaxInactiveInterval(Duration.between(Instant.now(clock), expiresAt.toInstant()));
        LOG.debug("save access token in session {}", session.getId());
    }

    private void saveRefreshToken(Session session, String refreshToken) {

        session.setAttribute(REFRESH_TOKEN, refreshToken);
        LOG.debug("save refresh token in session {}", session.getId());

    }

    private static class AccessTokenResponse {

        private static final ObjectMapper MAPPER = new ObjectMapper();

        private final SignedJWT accessToken;

        private final String refreshToken;

        @SneakyThrows
        public AccessTokenResponse(String body) {
            var bodyJson = MAPPER.readTree(body);

            Assert.notNull(bodyJson.get(ACCESS_TOKEN), "access token is missing");

            this.accessToken = SignedJWT.parse(bodyJson.path(ACCESS_TOKEN).textValue());
            this.refreshToken = bodyJson.path(REFRESH_TOKEN).textValue();

        }

        public boolean hasRefreshToken() {
            return refreshToken != null;
        }

    }
}
