package com.exemple.gateway.core.security.helper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SessionHelper {

    private final SessionRepository<Session> sessionRepository;

    public Optional<SessionCookie> extractSessionCookie(ServerHttpRequest request) {

        return extractAllSessionCookies(request).stream()
                .mapMulti((HttpCookie cookie, Consumer<SessionCookie> check) -> {
                    var session = sessionRepository.findById(cookie.getValue());
                    if (session != null) {
                        check.accept(new SessionCookie(cookie, session));
                    }
                })
                .findFirst();
    }

    public List<HttpCookie> extractAllSessionCookies(ServerHttpRequest request) {

        Map<String, List<HttpCookie>> emptyCookies = Collections.emptyMap();
        return ObjectUtils.getIfNull(request.getCookies(), CollectionUtils.toMultiValueMap(emptyCookies)).getOrDefault("JSESSIONID",
                Collections.emptyList());

    }

    public static record SessionCookie(HttpCookie cookie,
                                       Session session) {

    }
}
