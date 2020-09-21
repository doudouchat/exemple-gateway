package com.exemple.gateway.security.helper;

import java.util.Arrays;
import java.util.Optional;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.stereotype.Component;

@Component
public class SessionHelper {

    private final SessionRepository sessionRepository;

    public SessionHelper(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    public Optional<Pair<Cookie, Session>> extractSessionCookie(HttpServletRequest request) {

        return Arrays.stream((Cookie[]) ObjectUtils.defaultIfNull(request.getCookies(), new Cookie[0]))
                .filter(cookie -> "JSESSIONID".equals(cookie.getName())).map(cookie -> Pair.of(cookie, sessionRepository.findById(cookie.getValue())))
                .filter(p -> p.getRight() != null).findFirst();
    }

}
