package com.exemple.gateway.security.helper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.Pair;
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

    private final SessionRepository sessionRepository;

    public Optional<Pair<HttpCookie, Session>> extractSessionCookie(ServerHttpRequest request) {

        return extractAllSessionCookies(request).stream().map(cookie -> Pair.of(cookie, sessionRepository.findById(cookie.getValue())))
                .filter(p -> p.getRight() != null).findFirst();
    }

    public List<HttpCookie> extractAllSessionCookies(ServerHttpRequest request) {

        Map<String, List<HttpCookie>> emptyCookies = Collections.emptyMap();
        return ObjectUtils.defaultIfNull(request.getCookies(), CollectionUtils.toMultiValueMap(emptyCookies)).getOrDefault("JSESSIONID",
                Collections.emptyList());

    }
}
