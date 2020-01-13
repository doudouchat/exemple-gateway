package com.exemple.gateway.location;

import org.springframework.cloud.netflix.zuul.filters.post.LocationRewriteFilter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.netflix.zuul.context.RequestContext;

@Component
public class CustomLocationRewriteFilter extends LocationRewriteFilter {

    @Override
    public boolean shouldFilter() {
        RequestContext ctx = RequestContext.getCurrentContext();
        int statusCode = ctx.getResponseStatusCode();
        return super.shouldFilter() || HttpStatus.CREATED == HttpStatus.valueOf(statusCode);
    }

}
