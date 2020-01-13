package com.exemple.gateway.security.token;

import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.authentication.BearerTokenExtractor;
import org.springframework.security.oauth2.provider.authentication.TokenExtractor;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.session.SessionRepository;
import org.springframework.stereotype.Component;

@Component
public class CookieTokenExtractor implements TokenExtractor {

    private final TokenExtractor defaultTokenExtractor;

    private final SessionRepository<?> sessionRepository;

    public CookieTokenExtractor(SessionRepository<?> sessionRepository) {

        this.defaultTokenExtractor = new BearerTokenExtractor();
        this.sessionRepository = sessionRepository;
    }

    @Override
    public Authentication extract(HttpServletRequest request) {

        Authentication authentication = this.defaultTokenExtractor.extract(request);
        if (authentication == null && request.getCookies() != null) {

            authentication = Arrays.stream(request.getCookies()).filter(cookie -> "JSESSIONID".equals(cookie.getName())).findFirst()
                    .map(cookie -> sessionRepository.findById(cookie.getValue()))
                    .map(session -> new PreAuthenticatedAuthenticationToken(session.getAttribute("access_token"), "")).orElse(null);
        }

        return authentication;
    }

}
