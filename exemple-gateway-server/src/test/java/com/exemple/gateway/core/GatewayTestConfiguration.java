package com.exemple.gateway.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;

import com.auth0.jwt.algorithms.Algorithm;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

@Configuration
@Import({ GatewayConfiguration.class })
@EnableAutoConfiguration(exclude = UserDetailsServiceAutoConfiguration.class)
public class GatewayTestConfiguration {

    @Bean
    public HazelcastInstance hazelcastInstance(@Value("${hazelcast.port}") int port) {

        Config config = new Config();
        config.getNetworkConfig().setPort(port);
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(false);

        return Hazelcast.newHazelcastInstance(config);
    }

    @Bean
    public Algorithm algo() throws GeneralSecurityException, IOException {

        return Algorithm.RSA256(readPublicKey(), (RSAPrivateKey) readPrivateKey());

    }

    private static RSAPublicKey readPublicKey() throws GeneralSecurityException, IOException {

        String key = new String(Files.readAllBytes(new ClassPathResource("public_key").getFile().toPath()), StandardCharsets.UTF_8);

        String publicKeyPEM = key.replace("-----BEGIN PUBLIC KEY-----", "").replaceAll(System.lineSeparator(), "").replace("-----END PUBLIC KEY-----",
                "");

        byte[] encoded = Base64.decodeBase64(publicKeyPEM);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
        return (RSAPublicKey) keyFactory.generatePublic(keySpec);
    }

    private RSAPrivateKey readPrivateKey() throws GeneralSecurityException, IOException {

        String key = new String(Files.readAllBytes(new ClassPathResource("private_key").getFile().toPath()), StandardCharsets.UTF_8);

        String privateKeyPEM = key.replace("-----BEGIN PRIVATE KEY-----", "").replaceAll(System.lineSeparator(), "")
                .replace("-----END PRIVATE KEY-----", "");

        byte[] encoded = Base64.decodeBase64(privateKeyPEM);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
    }
}
