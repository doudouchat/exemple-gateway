package com.exemple.gateway.routing;

import java.io.InputStream;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.cloud.netflix.ribbon.support.RibbonRequestCustomizer;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandFactory;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonRoutingFilter;
import org.springframework.stereotype.Component;

@Component
public class CustomRoutingFilter extends RibbonRoutingFilter {

    @SuppressWarnings("rawtypes")
    public CustomRoutingFilter(ProxyRequestHelper helper, RibbonCommandFactory<?> ribbonCommandFactory,
            List<RibbonRequestCustomizer> requestCustomizers) {
        super(helper, ribbonCommandFactory, requestCustomizers);
    }

    @Override
    protected InputStream getRequestBody(HttpServletRequest request) {

        if (request.getContentLength() < 0) {
            return null;
        }

        return super.getRequestBody(request);
    }

}
