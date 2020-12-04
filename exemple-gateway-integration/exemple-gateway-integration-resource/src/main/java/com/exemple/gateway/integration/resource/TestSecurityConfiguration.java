package com.exemple.gateway.integration.resource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
public class TestSecurityConfiguration extends WebSecurityConfigurerAdapter {

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

        http

                .authorizeRequests()

                .antMatchers(HttpMethod.GET, "/**").hasAnyAuthority("SCOPE_test:read")

                .antMatchers(HttpMethod.POST, "/**").hasAnyAuthority("SCOPE_test:create")

                .antMatchers(HttpMethod.DELETE, "/**").hasAnyAuthority("SCOPE_test:delete")

                .antMatchers(HttpMethod.PATCH, "/**").hasAnyAuthority("SCOPE_test:update")

                .anyRequest().authenticated().and()

                .oauth2ResourceServer().jwt().and().and()

                .csrf().disable();

    }

}
