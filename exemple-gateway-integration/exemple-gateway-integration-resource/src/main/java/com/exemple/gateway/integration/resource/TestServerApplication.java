package com.exemple.gateway.integration.resource;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.provider.token.AccessTokenConverter;
import org.springframework.security.oauth2.provider.token.TokenStore;

@SpringBootApplication
public class TestServerApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(TestServerApplication.class);
    }

    @Configuration
    @EnableAuthorizationServer
    public static class TestAuthorizationConfiguration extends AuthorizationServerConfigurerAdapter {

        private final AuthenticationManager authenticationManager;

        private final TokenStore tokenStore;

        private final AccessTokenConverter accessTokenConverter;

        public TestAuthorizationConfiguration(AuthenticationManager authenticationManager, TokenStore tokenStore,
                AccessTokenConverter accessTokenConverter) {

            this.authenticationManager = authenticationManager;
            this.tokenStore = tokenStore;
            this.accessTokenConverter = accessTokenConverter;

        }

        @Override
        public void configure(ClientDetailsServiceConfigurer clients) throws Exception {

            String password = "{bcrypt}" + BCrypt.hashpw("secret", BCrypt.gensalt());

            clients.inMemory()

                    .withClient("resource").secret(password).authorizedGrantTypes("client_credentials", "password", "refresh_token")
                    .redirectUris("xxx").scopes("test:read", "test:create", "test:delete", "test:update")
                    .autoApprove("test:read", "test:create", "test:delete", "test:update");

        }

        @Override
        public void configure(AuthorizationServerEndpointsConfigurer endpoints) {

            endpoints.tokenStore(tokenStore).accessTokenConverter(accessTokenConverter).authenticationManager(authenticationManager);
        }

    }

}
