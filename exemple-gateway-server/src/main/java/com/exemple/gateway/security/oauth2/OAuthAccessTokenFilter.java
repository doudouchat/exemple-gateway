package com.exemple.gateway.security.oauth2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.servlet.http.Cookie;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;

import com.auth0.jwt.JWT;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;

@Component
public class OAuthAccessTokenFilter extends ZuulFilter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String ACCESS_TOKEN = "access_token";

    private static final String REFRESH_TOKEN = "refresh_token";

    private final SessionRepository repository;

    public OAuthAccessTokenFilter(SessionRepository repository) {
        this.repository = repository;
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
        return "POST".equals(context.getRequest().getMethod()) && StringUtils.endsWith(context.getRequest().getServletPath(), "/oauth/token");
    }

    @Override
    public Object run() throws ZuulException {
        RequestContext context = RequestContext.getCurrentContext();

        try {

            Session session = repository.createSession();

            Cookie sessionCookie = new Cookie("JSESSIONID", session.getId());
            sessionCookie.setHttpOnly(true);
            sessionCookie.setSecure(context.getRequest().isSecure());
            sessionCookie.setPath("/");

            String body = StreamUtils.copyToString(context.getResponseDataStream(), StandardCharsets.UTF_8);
            JsonNode node = MAPPER.readTree(body);

            if (!node.path(ACCESS_TOKEN).isMissingNode()) {
                session.setAttribute(ACCESS_TOKEN, node.get(ACCESS_TOKEN).textValue());
                Date expiresAt = JWT.decode(node.get(ACCESS_TOKEN).textValue()).getExpiresAt();
                Assert.notNull(expiresAt, "exp is required");
                sessionCookie.setMaxAge((int) Instant.now().until(expiresAt.toInstant(), ChronoUnit.SECONDS));
            }
            if (!node.path(REFRESH_TOKEN).isMissingNode()) {
                session.setAttribute(REFRESH_TOKEN, node.get(REFRESH_TOKEN).textValue());
            }
            repository.save(session);

            context.getResponse().addCookie(sessionCookie);
            context.setResponseBody(MAPPER.writeValueAsString(node));

        } catch (IOException e) {

            throw new ZuulException(e, HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }

        return null;
    }

}
