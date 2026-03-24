package com.personal.happygallery.config;

import com.personal.happygallery.common.crypto.BlindIndexer;
import com.personal.happygallery.common.crypto.FieldEncryptor;
import com.personal.happygallery.config.properties.FieldEncryptionProperties;
import java.util.HexFormat;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableConfigurationProperties(FieldEncryptionProperties.class)
public class CryptoConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public FieldEncryptor fieldEncryptor(FieldEncryptionProperties props) {
        return new FieldEncryptor(HexFormat.of().parseHex(props.encryptKey()));
    }

    @Bean
    public BlindIndexer blindIndexer(FieldEncryptionProperties props) {
        return new BlindIndexer(HexFormat.of().parseHex(props.hmacKey()));
    }
}
