package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.adapter.out.external.http.HttpPoolProperties;
import com.personal.happygallery.adapter.out.external.resilience.BoundedExecutorFactory;
import com.personal.happygallery.application.customer.port.out.EmailVerificationSender;
import com.personal.happygallery.application.customer.port.out.PhoneVerificationSender;
import com.personal.happygallery.application.notification.port.out.NotificationSendResult;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.mail.autoconfigure.MailProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;

/**
 * NHN Cloud Alimtalk / SMS 어댑터를 {@link ResilientNotificationSender}로 감싸 등록한다.
 *
 * <p>raw sender는 컨텍스트에 빈으로 노출하지 않고 데코레이터만 노출해야
 * {@code NotificationService}의 채널 fallback 체인에 같은 채널이 두 번 들어가지 않는다.
 */
@Configuration
@Profile("prod")
class NotificationResilienceConfig {

    private static final String SMTP_AUTH = "mail.smtp.auth";
    private static final String SMTP_CONNECTION_TIMEOUT = "mail.smtp.connectiontimeout";
    private static final String SMTP_READ_TIMEOUT = "mail.smtp.timeout";
    private static final String SMTP_WRITE_TIMEOUT = "mail.smtp.writetimeout";
    private static final String SMTP_STARTTLS_ENABLED = "mail.smtp.starttls.enable";
    private static final String SMTP_STARTTLS_REQUIRED = "mail.smtp.starttls.required";
    private static final String SMTP_CHECK_SERVER_IDENTITY = "mail.smtp.ssl.checkserveridentity";

    @Bean
    CircuitBreaker alimtalkNotificationCircuitBreaker(NotificationResilienceProperties properties,
                                                       CircuitBreakerRegistry circuitBreakerRegistry) {
        return circuitBreakerRegistry.circuitBreaker(
                "alimtalkNotification",
                circuitBreakerConfig(properties.circuitBreaker()));
    }

    @Bean
    CircuitBreaker smsNotificationCircuitBreaker(NotificationResilienceProperties properties,
                                                  CircuitBreakerRegistry circuitBreakerRegistry) {
        return circuitBreakerRegistry.circuitBreaker(
                "smsNotification",
                circuitBreakerConfig(properties.circuitBreaker()));
    }

    @Bean
    CircuitBreaker phoneVerificationSmsCircuitBreaker(NotificationResilienceProperties properties,
                                                       CircuitBreakerRegistry circuitBreakerRegistry) {
        return circuitBreakerRegistry.circuitBreaker(
                "phoneVerificationSms",
                circuitBreakerConfig(properties.circuitBreaker()));
    }

    @Bean
    CircuitBreaker emailVerificationCircuitBreaker(NotificationResilienceProperties properties,
                                                    CircuitBreakerRegistry circuitBreakerRegistry) {
        return circuitBreakerRegistry.circuitBreaker(
                "emailVerification",
                circuitBreakerConfig(properties.circuitBreaker()));
    }

    @Bean
    TimeLimiter notificationTimeLimiter(NotificationResilienceProperties properties) {
        return TimeLimiter.of(TimeLimiterConfig.custom()
                .timeoutDuration(properties.timeout())
                .cancelRunningFuture(true)
                .build());
    }

    @Bean
    TimeLimiter emailVerificationTimeLimiter(EmailVerificationProperties properties,
                                              MailProperties mailProperties) {
        validateEmailVerificationTransport(properties, mailProperties);
        return TimeLimiter.of(TimeLimiterConfig.custom()
                .timeoutDuration(properties.timeout())
                .cancelRunningFuture(true)
                .build());
    }

    @Bean
    ThreadPoolTaskExecutor alimtalkNotificationTimeoutExecutor(
            NotificationResilienceProperties properties,
            BoundedExecutorFactory executorFactory
    ) {
        return createExecutor(
                properties.alimtalkThreadPool(),
                executorFactory,
                "alimtalk-notification-timeout-",
                "happygallery.notification.alimtalk.executor.rejected",
                "Alimtalk notification timeout executor rejected task count");
    }

    @Bean
    ThreadPoolTaskExecutor smsNotificationTimeoutExecutor(
            NotificationResilienceProperties properties,
            BoundedExecutorFactory executorFactory
    ) {
        return createExecutor(
                properties.smsThreadPool(),
                executorFactory,
                "sms-notification-timeout-",
                "happygallery.notification.sms.executor.rejected",
                "SMS notification timeout executor rejected task count");
    }

    @Bean
    ThreadPoolTaskExecutor phoneVerificationTimeoutExecutor(
            NotificationResilienceProperties properties,
            BoundedExecutorFactory executorFactory
    ) {
        return createExecutor(
                properties.phoneVerificationThreadPool(),
                executorFactory,
                "phone-verification-timeout-",
                "happygallery.notification.phone_verification.executor.rejected",
                "Phone verification timeout executor rejected task count");
    }

    @Bean
    ThreadPoolTaskExecutor emailVerificationTimeoutExecutor(
            NotificationResilienceProperties properties,
            BoundedExecutorFactory executorFactory
    ) {
        return createExecutor(
                properties.emailVerificationThreadPool(),
                executorFactory,
                "email-verification-timeout-",
                "happygallery.notification.email_verification.executor.rejected",
                "Email verification timeout executor rejected task count");
    }

    @Bean
    @Order(1)
    NotificationSender kakaoNotificationSender(AlimtalkNotificationProperties props,
                                               @Qualifier("alimtalkRestClient") RestClient alimtalkRestClient,
                                               @Qualifier("alimtalkNotificationCircuitBreaker") CircuitBreaker circuitBreaker,
                                               @Qualifier("notificationTimeLimiter") TimeLimiter notificationTimeLimiter,
                                               @Qualifier("alimtalkNotificationTimeoutExecutor")
                                               Executor timeoutExecutor,
                                               NotificationResilienceProperties resilience) {
        validateTimeoutHierarchy(resilience, props);
        NhnAlimtalkSender raw = new NhnAlimtalkSender(props, alimtalkRestClient);
        return new ResilientNotificationSender(raw, circuitBreaker, notificationTimeLimiter,
                timeoutExecutor, resilience.timeout());
    }

    @Bean
    @Order(2)
    NotificationSender smsNotificationSender(SmsNotificationProperties props,
                                             @Qualifier("smsRestClient") RestClient smsRestClient,
                                             @Qualifier("smsNotificationCircuitBreaker") CircuitBreaker circuitBreaker,
                                             @Qualifier("notificationTimeLimiter") TimeLimiter notificationTimeLimiter,
                                             @Qualifier("smsNotificationTimeoutExecutor")
                                             Executor timeoutExecutor,
                                             NotificationResilienceProperties resilience) {
        validateTimeoutHierarchy(resilience, props);
        RealSmsSender raw = new RealSmsSender(props, smsRestClient);
        return new ResilientNotificationSender(raw, circuitBreaker, notificationTimeLimiter,
                timeoutExecutor, resilience.timeout());
    }

    @Bean
    PhoneVerificationSender phoneVerificationSender(SmsNotificationProperties props,
                                                     @Qualifier("smsRestClient") RestClient smsRestClient,
                                                     @Qualifier("phoneVerificationSmsCircuitBreaker")
                                                     CircuitBreaker circuitBreaker,
                                                     @Qualifier("notificationTimeLimiter") TimeLimiter notificationTimeLimiter,
                                                     @Qualifier("phoneVerificationTimeoutExecutor")
                                                     Executor timeoutExecutor,
                                                     NotificationResilienceProperties resilience) {
        validateTimeoutHierarchy(resilience, props);
        RealPhoneVerificationSender raw = new RealPhoneVerificationSender(props, smsRestClient);
        return new ResilientPhoneVerificationSender(
                raw,
                circuitBreaker,
                notificationTimeLimiter,
                timeoutExecutor,
                resilience.timeout());
    }

    @Bean
    EmailVerificationSender emailVerificationSender(
            EmailVerificationProperties props,
            JavaMailSender mailSender,
            @Qualifier("emailVerificationCircuitBreaker") CircuitBreaker circuitBreaker,
            @Qualifier("emailVerificationTimeLimiter") TimeLimiter timeLimiter,
            @Qualifier("emailVerificationTimeoutExecutor") Executor timeoutExecutor
    ) {
        RealEmailVerificationSender raw = new RealEmailVerificationSender(mailSender, props);
        return new ResilientEmailVerificationSender(
                raw,
                circuitBreaker,
                timeLimiter,
                timeoutExecutor,
                props.timeout());
    }

    private static void validateEmailVerificationTransport(
            EmailVerificationProperties properties,
            MailProperties mailProperties
    ) {
        Assert.hasText(mailProperties.getHost(), "이메일 인증 SMTP host는 필수입니다.");
        Assert.isTrue(mailProperties.getPort() != null && mailProperties.getPort() > 0,
                "이메일 인증 SMTP port는 0보다 커야 합니다.");
        Assert.hasText(mailProperties.getUsername(), "이메일 인증 SMTP username은 필수입니다.");
        Assert.hasText(mailProperties.getPassword(), "이메일 인증 SMTP password는 필수입니다.");
        Assert.hasText(properties.from(), "이메일 인증 발신 주소는 필수입니다.");

        boolean startTlsEnabled = booleanMailProperty(mailProperties, SMTP_STARTTLS_ENABLED);
        boolean startTlsRequired = booleanMailProperty(mailProperties, SMTP_STARTTLS_REQUIRED);
        boolean sslEnabled = mailProperties.getSsl().isEnabled();
        Assert.isTrue(
                startTlsEnabled ^ sslEnabled,
                "이메일 인증 SMTP는 STARTTLS 또는 SSL 중 하나만 활성화해야 합니다.");
        Assert.isTrue(!startTlsEnabled || startTlsRequired,
                "이메일 인증 SMTP STARTTLS는 required로 설정해야 합니다.");
        Assert.isTrue(mailProperties.getSsl().isVerifyHostname()
                        && booleanMailProperty(mailProperties, SMTP_CHECK_SERVER_IDENTITY),
                "이메일 인증 SMTP는 서버 인증서 호스트명을 검증해야 합니다.");
        Assert.isTrue(booleanMailProperty(mailProperties, SMTP_AUTH),
                "이메일 인증 SMTP 인증은 활성화해야 합니다.");

        Duration transportTimeout = durationMailProperty(mailProperties, SMTP_CONNECTION_TIMEOUT)
                .plus(durationMailProperty(mailProperties, SMTP_READ_TIMEOUT))
                .plus(durationMailProperty(mailProperties, SMTP_WRITE_TIMEOUT));
        Assert.isTrue(
                properties.timeout().compareTo(transportTimeout) > 0,
                "이메일 인증 외부 timeout은 SMTP transport timeout 합보다 커야 합니다.");
    }

    private static CircuitBreakerConfig circuitBreakerConfig(NotificationResilienceProperties.CircuitBreaker cb) {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(cb.failureRateThreshold())
                .slidingWindowSize(cb.slidingWindowSize())
                .minimumNumberOfCalls(cb.minimumNumberOfCalls())
                .waitDurationInOpenState(cb.waitDurationOpen())
                .permittedNumberOfCallsInHalfOpenState(cb.permittedCallsInHalfOpenState())
                .recordResult(NotificationResilienceConfig::isFailureResult)
                .build();
    }

    private static boolean isFailureResult(Object result) {
        return result == NotificationSendResult.TRANSIENT_FAILURE
                || result == NotificationSendResult.DELIVERY_UNKNOWN;
    }

    private static ThreadPoolTaskExecutor createExecutor(
            NotificationResilienceProperties.ThreadPool threadPool,
            BoundedExecutorFactory executorFactory,
            String threadNamePrefix,
            String rejectionMetricName,
            String rejectionMetricDescription
    ) {
        return executorFactory.create(
                threadPool.poolSize(),
                threadPool.queueCapacity(),
                threadNamePrefix,
                rejectionMetricName,
                rejectionMetricDescription);
    }

    private static void validateTimeoutHierarchy(NotificationResilienceProperties resilience,
                                                 HttpPoolProperties transport) {
        Duration transportBudget = transport.acquireTimeout()
                .plus(transport.connectTimeout())
                .plus(transport.timeout());
        Assert.isTrue(resilience.timeout().compareTo(transportBudget) > 0,
                "알림 TimeLimiter는 acquire + connect + response timeout 합보다 커야 합니다.");
    }

    private static Duration durationMailProperty(MailProperties properties, String key) {
        String value = requiredMailProperty(properties.getProperties(), key);
        try {
            int millis = Integer.parseInt(value);
            Assert.isTrue(millis > 0, key + "는 0보다 커야 합니다.");
            return Duration.ofMillis(millis);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(key + "는 밀리초 단위 정수여야 합니다.", exception);
        }
    }

    private static boolean booleanMailProperty(MailProperties properties, String key) {
        String value = requiredMailProperty(properties.getProperties(), key);
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        throw new IllegalArgumentException(key + "는 true 또는 false여야 합니다.");
    }

    private static String requiredMailProperty(Map<String, String> properties, String key) {
        String value = properties.get(key);
        Assert.hasText(value, key + " 설정은 필수입니다.");
        return value.trim();
    }
}
