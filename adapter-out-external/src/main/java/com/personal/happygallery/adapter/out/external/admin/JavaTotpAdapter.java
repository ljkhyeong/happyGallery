package com.personal.happygallery.adapter.out.external.admin;

import com.personal.happygallery.application.admin.port.out.AdminTotpPort;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.recovery.RecoveryCodeGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class JavaTotpAdapter implements AdminTotpPort {

    private static final String ISSUER = "해피갤러리 관리자";
    private static final int CODE_DIGITS = 6;
    private static final int PERIOD_SECONDS = 30;

    private final Clock clock;
    private final DefaultSecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final RecoveryCodeGenerator recoveryCodeGenerator = new RecoveryCodeGenerator();

    public JavaTotpAdapter(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Enrollment generateEnrollment(String username) {
        String secret = secretGenerator.generate();
        QrData qrData = new QrData.Builder()
                .label(username)
                .secret(secret)
                .issuer(ISSUER)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(CODE_DIGITS)
                .period(PERIOD_SECONDS)
                .build();
        return new Enrollment(secret, qrData.getUri());
    }

    @Override
    public boolean verify(String secret, String code) {
        DefaultCodeVerifier verifier = new DefaultCodeVerifier(
                new DefaultCodeGenerator(HashingAlgorithm.SHA1, CODE_DIGITS),
                () -> clock.instant().getEpochSecond());
        verifier.setTimePeriod(PERIOD_SECONDS);
        verifier.setAllowedTimePeriodDiscrepancy(1);
        return verifier.isValidCode(secret, code);
    }

    @Override
    public List<String> generateRecoveryCodes(int count) {
        return List.of(recoveryCodeGenerator.generateCodes(count));
    }
}
