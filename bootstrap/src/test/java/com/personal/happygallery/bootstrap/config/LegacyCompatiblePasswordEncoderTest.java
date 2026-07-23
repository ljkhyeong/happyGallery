package com.personal.happygallery.bootstrap.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyCompatiblePasswordEncoderTest {

    @DisplayName("기존 BCrypt로 쓰면서 기존 형식과 식별자 형식을 모두 읽는다")
    @Test
    void supportsBothBcryptFormatsWithoutChangingWriteFormat() {
        String rawPassword = "password123";
        LegacyCompatiblePasswordEncoder passwordEncoder =
                new LegacyCompatiblePasswordEncoder(new BCryptPasswordEncoder());

        String encoded = passwordEncoder.encode(rawPassword);

        assertThat(encoded).startsWith("$2");
        assertThat(passwordEncoder.matches(rawPassword, encoded)).isTrue();
        assertThat(passwordEncoder.matches(rawPassword, "{bcrypt}" + encoded)).isTrue();
        assertThat(passwordEncoder.upgradeEncoding(encoded)).isFalse();
    }
}
