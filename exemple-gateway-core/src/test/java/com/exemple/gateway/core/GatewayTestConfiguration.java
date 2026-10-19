package com.exemple.gateway.core;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;

import org.apache.commons.codec.binary.Base64;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import com.nimbusds.jose.JWSSignerOption;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.opts.AllowWeakRSAKey;

import reactor.core.publisher.Mono;

@Configuration
@Import({ GatewayConfiguration.class })
@EnableAutoConfiguration(exclude = UserDetailsServiceAutoConfiguration.class)
public class GatewayTestConfiguration {

    @Bean
    @Primary
    public Clock fixed() {
        return Clock.fixed(Instant.now(), ZoneId.systemDefault());
    }

    @Bean
    @Primary
    public ReactiveOAuth2AuthorizedClientManager authorizedClientManagerMock() {
        ReactiveOAuth2AuthorizedClientManager authorizedClientManager = mock(ReactiveOAuth2AuthorizedClientManager.class);

        OAuth2AuthorizedClient oAuth2AuthorizedClient = new OAuth2AuthorizedClient(
                ClientRegistration.withRegistrationId("exemple")
                        .clientId("test-client-id")
                        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                        .tokenUri("http://localhost/token")
                        .build(),
                "test",
                new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "test-access-token", Instant.now(), Instant.now().plusSeconds(3600)));

        when(authorizedClientManager.authorize(any())).thenReturn(Mono.just(oAuth2AuthorizedClient));

        return authorizedClientManager;
    }

    @Bean
    public RSASSASigner signer() throws GeneralSecurityException, IOException {
        return new RSASSASigner(readPrivateKey(), Collections.singleton((JWSSignerOption) AllowWeakRSAKey.getInstance()));
    }

    private RSAPrivateKey readPrivateKey() throws GeneralSecurityException, IOException {

        String key = new String(Files.readAllBytes(new ClassPathResource("private_key").getFile().toPath()));

        String privateKeyPEM = key.replace("-----BEGIN PRIVATE KEY-----", "").replaceAll(System.lineSeparator(), "")
                .replace("-----END PRIVATE KEY-----", "");

        byte[] encoded = Base64.decodeBase64(privateKeyPEM);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
    }
}
