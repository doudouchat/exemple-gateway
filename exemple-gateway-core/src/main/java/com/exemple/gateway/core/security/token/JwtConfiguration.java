package com.exemple.gateway.core.security.token;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

import com.exemple.gateway.core.security.token.validator.NotBlackListTokenValidator;

@Configuration
public class JwtConfiguration {

    public JwtConfiguration(ReactiveJwtDecoder reactiveJwtDecoder, NotBlackListTokenValidator notBlackListTokenValidator) {

        OAuth2TokenValidator<Jwt> jwtValidator = new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(), notBlackListTokenValidator);
        ((NimbusReactiveJwtDecoder) reactiveJwtDecoder).setJwtValidator(jwtValidator);

    }
}
