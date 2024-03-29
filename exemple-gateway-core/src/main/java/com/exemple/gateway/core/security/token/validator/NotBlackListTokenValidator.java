package com.exemple.gateway.core.security.token.validator;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.hazelcast.core.HazelcastInstance;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NotBlackListTokenValidator implements OAuth2TokenValidator<Jwt> {

    public static final String TOKEN_BLACK_LIST = "token.black_list";

    private final HazelcastInstance hazelcastInstance;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {

        return isTokenInBlackList(token)
                ? OAuth2TokenValidatorResult.failure(new OAuth2Error(OAuth2ErrorCodes.ACCESS_DENIED, "Jwt is excluded", null))
                : OAuth2TokenValidatorResult.success();
    }

    private boolean isTokenInBlackList(Jwt token) {

        return token.getId() != null && hazelcastInstance.getMap(TOKEN_BLACK_LIST).containsKey(token.getId());
    }

}
