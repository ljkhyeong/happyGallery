package com.personal.happygallery.bootstrap.config;

import com.personal.happygallery.domain.crypto.BlindIndexer;
import com.personal.happygallery.domain.crypto.FieldEncryptor;
import com.personal.happygallery.bootstrap.config.properties.FieldEncryptionProperties;
import java.util.HexFormat;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
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
