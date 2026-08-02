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
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
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
                .timeoutDuration(Duration.ofMillis(properties.timeoutMillis()))
                .cancelRunningFuture(true)
                .build());
    }

    @Bean
    TimeLimiter emailVerificationTimeLimiter(EmailVerificationProperties properties) {
        validateEmailVerificationTransport(properties);
        return TimeLimiter.of(TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofMillis(properties.timeoutMillis()))
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
                timeoutExecutor, resilience.timeoutMillis());
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
                timeoutExecutor, resilience.timeoutMillis());
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
                resilience.timeoutMillis());
    }

    @Bean
    EmailVerificationSender emailVerificationSender(
            EmailVerificationProperties props,
            @Qualifier("emailVerificationMailSender") JavaMailSender mailSender,
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
                props.timeoutMillis());
    }

    @Bean
    JavaMailSender emailVerificationMailSender(EmailVerificationProperties props) {
        validateEmailVerificationTransport(props);
        Assert.hasText(props.host(), "이메일 인증 SMTP host는 필수입니다.");
        Assert.hasText(props.username(), "이메일 인증 SMTP username은 필수입니다.");
        Assert.hasText(props.password(), "이메일 인증 SMTP password는 필수입니다.");
        Assert.hasText(props.from(), "이메일 인증 발신 주소는 필수입니다.");
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(props.host());
        mailSender.setPort(props.port());
        mailSender.setUsername(props.username());
        mailSender.setPassword(props.password());
        mailSender.getJavaMailProperties().setProperty(
                "mail.smtp.connectiontimeout",
                String.valueOf(props.connectionTimeoutMillis()));
        mailSender.getJavaMailProperties().setProperty(
                "mail.smtp.timeout",
                String.valueOf(props.readTimeoutMillis()));
        mailSender.getJavaMailProperties().setProperty(
                "mail.smtp.writetimeout",
                String.valueOf(props.writeTimeoutMillis()));
        mailSender.getJavaMailProperties().setProperty(
                "mail.smtp.starttls.enable",
                String.valueOf(props.startTlsEnabled()));
        mailSender.getJavaMailProperties().setProperty(
                "mail.smtp.starttls.required",
                String.valueOf(props.startTlsEnabled()));
        mailSender.getJavaMailProperties().setProperty(
                "mail.smtp.ssl.enable",
                String.valueOf(props.sslEnabled()));
        mailSender.getJavaMailProperties().setProperty(
                "mail.smtp.ssl.checkserveridentity",
                "true");
        mailSender.getJavaMailProperties().setProperty("mail.smtp.auth", "true");
        return mailSender;
    }

    private static void validateEmailVerificationTransport(
            EmailVerificationProperties properties
    ) {
        Assert.isTrue(
                properties.startTlsEnabled() ^ properties.sslEnabled(),
                "이메일 인증 SMTP는 STARTTLS 또는 SSL 중 하나만 활성화해야 합니다.");
        long transportTimeoutMillis = (long) properties.connectionTimeoutMillis()
                + properties.readTimeoutMillis()
                + properties.writeTimeoutMillis();
        Assert.isTrue(
                properties.timeoutMillis() > transportTimeoutMillis,
                "이메일 인증 외부 timeout은 SMTP transport timeout 합보다 커야 합니다.");
    }

    private static CircuitBreakerConfig circuitBreakerConfig(NotificationResilienceProperties.CircuitBreaker cb) {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(cb.failureRateThreshold())
                .slidingWindowSize(cb.slidingWindowSize())
                .minimumNumberOfCalls(cb.minimumNumberOfCalls())
                .waitDurationInOpenState(Duration.ofSeconds(cb.waitDurationOpenSeconds()))
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
        long transportBudgetMillis = transport.acquireTimeoutMillis()
                + transport.connectTimeoutMillis()
                + transport.timeoutMillis();
        Assert.isTrue(resilience.timeoutMillis() > transportBudgetMillis,
                "알림 TimeLimiter는 acquire + connect + response timeout 합보다 커야 합니다.");
    }
}
