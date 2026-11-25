package com.exemple.gateway.core.security.oauth2;

import org.springframework.security.oauth2.client.ClientAuthorizationException;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.core.OAuth2Error;

import com.hazelcast.core.HazelcastInstance;
import com.nimbusds.jwt.SignedJWT;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class NotBlackListTokenReactiveOAuth2AuthorizedClientProvider implements ReactiveOAuth2AuthorizedClientProvider {

    public static final String TOKEN_BLACK_LIST = "token.black_list";

    private final ReactiveOAuth2AuthorizedClientProvider delegate;

    private final HazelcastInstance hazelcastInstance;

    @Override
    @SneakyThrows
    public Mono<OAuth2AuthorizedClient> authorize(OAuth2AuthorizationContext context) {

        var authorizedClient = context.getAuthorizedClient();

        if (authorizedClient != null) {

            var signedJwt = SignedJWT.parse(authorizedClient.getAccessToken().getTokenValue());

            if (isTokenInBlackList(signedJwt)) {

                var exception = new ClientAuthorizationException(
                        new OAuth2Error("token revoked", "toto", null), context.getClientRegistration().getClientId());

                return Mono.error(exception);

            }

        }

        return delegate.authorize(context);
    }

    private boolean isTokenInBlackList(SignedJWT signedJwt) {

        return signedJwt.getHeader().getKeyID() != null && hazelcastInstance.getMap(TOKEN_BLACK_LIST).containsKey(signedJwt.getHeader().getKeyID());
    }

}
