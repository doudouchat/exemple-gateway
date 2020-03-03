package com.exemple.gateway.security;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.authentication.BearerTokenExtractor;
import org.springframework.security.oauth2.provider.authentication.TokenExtractor;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableResourceServer
@ComponentScan(basePackages = "com.exemple.gateway.security")
public class GatewaySecurityConfiguration extends ResourceServerConfigurerAdapter {

    private final String[] excludes;

    private final TokenExtractor tokenExtractor;

    public GatewaySecurityConfiguration(TokenExtractor tokenExtractor, @Value("${gateway.security.excludes:}") String... excludes) {

        this.tokenExtractor = tokenExtractor;
        this.excludes = excludes.clone();
    }

    @Override
    public void configure(ResourceServerSecurityConfigurer config) {
        config.resourceId(null).tokenExtractor(tokenExtractor);
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {

        CookieCsrfTokenRepository csrfTokenRepository = new CookieCsrfTokenRepository();
        csrfTokenRepository.setCookieHttpOnly(false);

        CheckTokenExtractor checkToken = new CheckTokenExtractor();

        http.cors().and().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.NEVER).and()

                .authorizeRequests()

                .regexMatchers(excludes).permitAll()

                .anyRequest().authenticated().and()

                .csrf().csrfTokenRepository(csrfTokenRepository).ignoringAntMatchers("/ExempleAuthorization/oauth/**")
                .ignoringRequestMatchers(checkToken::hasBearerToken);
    }

    private static class CheckTokenExtractor extends BearerTokenExtractor {

        public boolean hasBearerToken(HttpServletRequest request) {

            return StringUtils.isNotBlank(this.extractToken(request));

        }

    }
}
