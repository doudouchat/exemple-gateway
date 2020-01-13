package com.exemple.gateway.core;

import java.io.IOException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.osjava.sj.SimpleJndi;
import org.osjava.sj.loader.JndiLoader;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jndi.JndiObjectFactoryBean;

import com.exemple.gateway.core.property.GatewayPropertyConfiguration;

@Configuration
@Import({ GatewayConfiguration.class })
@EnableAutoConfiguration(exclude = UserDetailsServiceAutoConfiguration.class)
public class GatewayTestConfiguration extends GatewayPropertyConfiguration {

    @Bean
    public InitialContext initialContext() throws NamingException, IOException {

        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.osjava.sj.SimpleContextFactory");
        System.setProperty(SimpleJndi.ENC, "java:comp");
        System.setProperty(JndiLoader.COLON_REPLACE, "--");
        System.setProperty(JndiLoader.DELIMITER, "/");
        System.setProperty(SimpleJndi.SHARED, "true");
        System.setProperty(SimpleJndi.ROOT, new ClassPathResource("java--comp").getURL().getFile());

        return new InitialContext();

    }

    @Bean
    @DependsOn("initialContext")
    @Override
    public JndiObjectFactoryBean jndiObjectFactoryBean() {

        return super.jndiObjectFactoryBean();
    }
}
