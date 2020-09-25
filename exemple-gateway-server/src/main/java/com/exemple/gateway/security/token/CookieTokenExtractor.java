package com.exemple.gateway.security.token;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.authentication.BearerTokenExtractor;
import org.springframework.security.oauth2.provider.authentication.TokenExtractor;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

import com.exemple.gateway.security.helper.SessionHelper;

@Component
public class CookieTokenExtractor implements TokenExtractor {

    private final TokenExtractor defaultTokenExtractor;

    private final SessionHelper sessionHelper;

    private final List<RequestMatcher> excludes;

    public CookieTokenExtractor(SessionHelper sessionHelper, @Value("${gateway.security.excludes:}") String[] excludes) {

        this.defaultTokenExtractor = new BearerTokenExtractor();
        this.sessionHelper = sessionHelper;
        this.excludes = Arrays.stream(excludes).map(exclude -> new RegexRequestMatcher(exclude, null)).collect(Collectors.toList());
    }

    @Override
    public Authentication extract(HttpServletRequest request) {

        if (isNonAuthenticatedRequest(request)) {
            return null;
        }

        Authentication authentication = this.defaultTokenExtractor.extract(request);
        if (authentication == null) {

            authentication = sessionHelper.extractSessionCookie(request).map(Pair::getValue)
                    .map(session -> new PreAuthenticatedAuthenticationToken(session.getAttribute("access_token"), "")).orElse(null);

        }

        return authentication;
    }

    private boolean isNonAuthenticatedRequest(HttpServletRequest request) {

        return this.excludes.stream().anyMatch(exclude -> exclude.matches(request));
    }

}
