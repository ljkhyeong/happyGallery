package com.personal.happygallery.bootstrap.keyrotation;

import com.personal.happygallery.application.crypto.rotation.KeyRotationUseCase;
import com.personal.happygallery.bootstrap.config.properties.KeyRotationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.key-rotation", name = "enabled", havingValue = "true")
public class KeyRotationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(KeyRotationRunner.class);

    private final KeyRotationUseCase keyRotationUseCase;
    private final KeyRotationProperties properties;
    private final ConfigurableApplicationContext context;

    public KeyRotationRunner(KeyRotationUseCase keyRotationUseCase,
                             KeyRotationProperties properties,
                             ConfigurableApplicationContext context) {
        this.keyRotationUseCase = keyRotationUseCase;
        this.properties = properties;
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments args) {
        var result = keyRotationUseCase.rotate(properties.sourceKeyId());
        log.info("키 회전 완료 [users={}, guests={}, bookings={}, attempts={}, fulfillments={}, "
                        + "smartStoreOrders={}, social={}, "
                        + "adminMfa={}, deletedPhoneVerifications={}, deletedEmailVerifications={}, "
                        + "pendingSocial={}, pendingAdminMfa={}]",
                result.users(), result.guests(), result.bookings(),
                result.paymentAttempts(), result.fulfillments(),
                result.smartStoreOrders(),
                result.socialAccounts(), result.adminMfaSecrets(),
                result.deletedPhoneVerifications(), result.deletedEmailVerifications(),
                result.pendingSocialAccounts(),
                result.pendingAdminMfaSecrets());
        SpringApplication.exit(context);
    }
}
