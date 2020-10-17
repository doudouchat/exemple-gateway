package com.exemple.gateway.security.oauth2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.servlet.http.Cookie;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.http.HttpStatus;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;

import com.auth0.jwt.JWT;
import com.exemple.gateway.security.helper.SessionHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;

@Component
public class OAuthAccessTokenFilter extends ZuulFilter {

    private static final Logger LOG = LoggerFactory.getLogger(OAuthAccessTokenFilter.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String ACCESS_TOKEN = "access_token";

    private static final String REFRESH_TOKEN = "refresh_token";

    private final SessionRepository repository;

    private final SessionHelper sessionHelper;

    public OAuthAccessTokenFilter(SessionRepository repository, SessionHelper sessionHelper) {
        this.repository = repository;
        this.sessionHelper = sessionHelper;
    }

    @Override
    public String filterType() {
        return FilterConstants.POST_TYPE;
    }

    @Override
    public int filterOrder() {
        return FilterConstants.SEND_RESPONSE_FILTER_ORDER - 1;
    }

    @Override
    public boolean shouldFilter() {
        RequestContext context = RequestContext.getCurrentContext();
        int statusCode = context.getResponseStatusCode();
        return "POST".equals(context.getRequest().getMethod()) && HttpStatus.valueOf(statusCode).is2xxSuccessful()
                && StringUtils.endsWith(context.getRequest().getServletPath(), "/oauth/token");
    }

    @Override
    public Object run() throws ZuulException {
        RequestContext context = RequestContext.getCurrentContext();

        try {

            Pair<Cookie, Session> cookieAndSession = sessionHelper.extractSessionCookie(context.getRequest()).orElseGet(() -> {
                Session session = repository.createSession();
                Cookie sessionCookie = createSessionCookie(context, session.getId());
                context.getResponse().addCookie(sessionCookie);
                return Pair.of(sessionCookie, session);
            });

            Session session = cookieAndSession.getRight();
            Cookie sessionCookie = cookieAndSession.getLeft();

            LOG.debug("session cookie is {}", session.getId());

            JsonNode authorizationNode = extractAuthorizationNode(context);

            saveTokens(session, authorizationNode);
            saveMaxAge(sessionCookie, authorizationNode);

            context.setResponseBody(MAPPER.writeValueAsString(authorizationNode));

        } catch (IOException e) {

            throw new ZuulException(e, HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
        }

        return null;
    }

    private static Cookie createSessionCookie(RequestContext context, String sessionId) {

        Cookie sessionCookie = new Cookie("JSESSIONID", sessionId);
        sessionCookie.setHttpOnly(true);
        sessionCookie.setSecure(context.getRequest().isSecure());
        sessionCookie.setPath("/");

        LOG.debug("new session cookie {}", sessionId);

        return sessionCookie;
    }

    private static JsonNode extractAuthorizationNode(RequestContext context) throws IOException {

        String body = StreamUtils.copyToString(context.getResponseDataStream(), StandardCharsets.UTF_8);
        return MAPPER.readTree(body);
    }

    private void saveTokens(Session session, JsonNode authorizationNode) {

        if (!authorizationNode.path(ACCESS_TOKEN).isMissingNode()) {
            session.setAttribute(ACCESS_TOKEN, authorizationNode.get(ACCESS_TOKEN).textValue());
            LOG.debug("save access token in session {}", session.getId());

        }

        if (!authorizationNode.path(REFRESH_TOKEN).isMissingNode()) {
            session.setAttribute(REFRESH_TOKEN, authorizationNode.get(REFRESH_TOKEN).textValue());
            LOG.debug("save refresh token in session {}", session.getId());

        }

        repository.save(session);

    }

    private static void saveMaxAge(Cookie sessionCookie, JsonNode authorizationNode) {

        if (!authorizationNode.path(ACCESS_TOKEN).isMissingNode()) {
            Date expiresAt = JWT.decode(authorizationNode.get(ACCESS_TOKEN).textValue()).getExpiresAt();
            Assert.notNull(expiresAt, "exp is required");
            sessionCookie.setMaxAge((int) Instant.now().until(expiresAt.toInstant(), ChronoUnit.SECONDS));
        }
    }

}
