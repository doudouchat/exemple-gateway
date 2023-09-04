package com.exemple.gateway.core;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.nimbusds.jose.JWSSignerOption;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.opts.AllowWeakRSAKey;

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
    public HazelcastInstance hazelcastInstance(@Value("${hazelcast.port}") int port) {

        Config config = new Config();
        config.getNetworkConfig().setPort(port);
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(false);

        return Hazelcast.newHazelcastInstance(config);
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
