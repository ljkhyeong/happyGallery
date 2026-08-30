package com.personal.happygallery.adapter.out.external.admin;

import com.personal.happygallery.application.admin.port.out.AdminTotpPort;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.CodeGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.recovery.RecoveryCodeGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.List;
import java.util.OptionalLong;
import org.springframework.stereotype.Component;

@Component
public class JavaTotpAdapter implements AdminTotpPort {

    private static final String ISSUER = "해피갤러리 관리자";
    private static final int CODE_DIGITS = 6;
    private static final int PERIOD_SECONDS = 30;
    private static final int ALLOWED_TIME_STEP_DISCREPANCY = 1;

    private final Clock clock;
    private final DefaultCodeGenerator codeGenerator =
            new DefaultCodeGenerator(HashingAlgorithm.SHA1, CODE_DIGITS);
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
    public OptionalLong findMatchingTimeStep(String secret, String code) {
        long currentTimeStep = Math.floorDiv(clock.instant().getEpochSecond(), PERIOD_SECONDS);
        long matchedTimeStep = Long.MIN_VALUE;

        for (int offset = -ALLOWED_TIME_STEP_DISCREPANCY;
             offset <= ALLOWED_TIME_STEP_DISCREPANCY;
             offset++) {
            long candidateTimeStep = currentTimeStep + offset;
            if (matches(secret, code, candidateTimeStep)) {
                matchedTimeStep = candidateTimeStep;
            }
        }
        return matchedTimeStep == Long.MIN_VALUE
                ? OptionalLong.empty()
                : OptionalLong.of(matchedTimeStep);
    }

    @Override
    public List<String> generateRecoveryCodes(int count) {
        return List.of(recoveryCodeGenerator.generateCodes(count));
    }

    private boolean matches(String secret, String code, long timeStep) {
        try {
            String generated = codeGenerator.generate(secret, timeStep);
            return MessageDigest.isEqual(
                    generated.getBytes(StandardCharsets.US_ASCII),
                    code.getBytes(StandardCharsets.US_ASCII));
        } catch (CodeGenerationException exception) {
            return false;
        }
    }
}
