package com.exemple.gateway.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.provider.token.AccessTokenConverter;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@SpringBootApplication
public class TestServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestServerApplication.class, args);
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
                    .redirectUris("xxx").scopes("test").autoApprove("test");

        }

        @Override
        public void configure(AuthorizationServerEndpointsConfigurer endpoints) {

            endpoints.tokenStore(tokenStore).accessTokenConverter(accessTokenConverter).authenticationManager(authenticationManager);
        }

    }

    @Configuration
    public static class TestWebSecurityConfig extends WebSecurityConfigurerAdapter {

        @Override
        @Bean
        public AuthenticationManager authenticationManagerBean() throws Exception {
            return super.authenticationManagerBean();
        }

        @Bean
        @Override
        protected UserDetailsService userDetailsService() {

            String password = "{bcrypt}" + BCrypt.hashpw("123", BCrypt.gensalt());

            InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
            manager.createUser(User.withUsername("jean.dupond@gmail.com").password(password).authorities("USER").build());

            return manager;
        }

        @Override
        protected void configure(final HttpSecurity http) throws Exception {

            http.authorizeRequests().antMatchers("/**").permitAll().and().csrf().disable();
        }
    }

    @Configuration
    public static class TestTokenConfiguration {

        @Bean
        public TokenStore tokenStore() {
            return new JwtTokenStore(accessTokenConverter());
        }

        @Bean
        public JwtAccessTokenConverter accessTokenConverter() {
            JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
            converter.setSigningKey("abc");
            return converter;
        }

        @Bean
        @Primary
        public DefaultTokenServices tokenServices() {
            DefaultTokenServices defaultTokenServices = new DefaultTokenServices();
            defaultTokenServices.setTokenStore(tokenStore());
            defaultTokenServices.setSupportRefreshToken(true);
            return defaultTokenServices;
        }

    }

}
