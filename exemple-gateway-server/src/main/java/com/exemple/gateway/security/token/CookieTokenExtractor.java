package com.exemple.gateway.security.token;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.authentication.BearerTokenExtractor;
import org.springframework.security.oauth2.provider.authentication.TokenExtractor;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;

import com.exemple.gateway.security.helper.SessionHelper;

@Component
public class CookieTokenExtractor implements TokenExtractor {

    private final TokenExtractor defaultTokenExtractor;

    private final SessionHelper sessionHelper;

    public CookieTokenExtractor(SessionHelper sessionHelper) {

        this.defaultTokenExtractor = new BearerTokenExtractor();
        this.sessionHelper = sessionHelper;
    }

    @Override
    public Authentication extract(HttpServletRequest request) {

        Authentication authentication = this.defaultTokenExtractor.extract(request);
        if (authentication == null) {

            authentication = sessionHelper.extractSessionCookie(request).map(Pair::getValue)
                    .map(session -> new PreAuthenticatedAuthenticationToken(session.getAttribute("access_token"), "")).orElse(null);

        }

        return authentication;
    }

}
