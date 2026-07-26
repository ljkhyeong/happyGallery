package com.personal.happygallery.adapter.out.external.admin;

import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.HashingAlgorithm;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JavaTotpAdapterTest {

    @DisplayName("등록 비밀키로 TOTP를 검증하고 정해진 형식의 복구 코드를 생성한다")
    @Test
    void enrollmentSecret_verifiesCurrentTotp() throws Exception {
        Clock clock = Clock.fixed(Instant.parse("2026-07-26T07:00:00Z"), ZoneOffset.UTC);
        JavaTotpAdapter adapter = new JavaTotpAdapter(clock);
        var enrollment = adapter.generateEnrollment("admin");
        long counter = Math.floorDiv(clock.instant().getEpochSecond(), 30);
        String code = new DefaultCodeGenerator(HashingAlgorithm.SHA1, 6)
                .generate(enrollment.secret(), counter);

        assertThat(adapter.verify(enrollment.secret(), code)).isTrue();
        assertThat(adapter.verify(enrollment.secret(), "000000")).isFalse();
        assertThat(enrollment.provisioningUri()).startsWith("otpauth://totp/");
        assertThat(adapter.generateRecoveryCodes(10))
                .hasSize(10)
                .allMatch(recoveryCode ->
                        recoveryCode.matches("[a-z0-9]{4}(?:-[a-z0-9]{4}){3}"));
    }
}
