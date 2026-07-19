package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.adapter.out.external.http.HttpPoolProperties;
import com.personal.happygallery.application.customer.port.out.PhoneVerificationSender;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
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
    CircuitBreaker alimtalkNotificationCircuitBreaker(NotificationResilienceProperties properties) {
        return CircuitBreaker.of("alimtalkNotification", circuitBreakerConfig(properties.circuitBreaker()));
    }

    @Bean
    CircuitBreaker smsNotificationCircuitBreaker(NotificationResilienceProperties properties) {
        return CircuitBreaker.of("smsNotification", circuitBreakerConfig(properties.circuitBreaker()));
    }

    @Bean
    TimeLimiter notificationTimeLimiter(NotificationResilienceProperties properties) {
        return TimeLimiter.of(TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofMillis(properties.timeoutMillis()))
                .cancelRunningFuture(true)
                .build());
    }

    @Bean(destroyMethod = "shutdown")
    ExecutorService notificationTimeoutExecutor(NotificationResilienceProperties properties,
                                                MeterRegistry meterRegistry) {
        NotificationResilienceProperties.ThreadPool threadPool = properties.threadPool();
        Counter rejectedCounter = Counter.builder("happygallery.notification.executor.rejected")
                .description("Notification timeout executor rejected task count")
                .register(meterRegistry);
        RejectedExecutionHandler abortPolicy = new ThreadPoolExecutor.AbortPolicy();
        RejectedExecutionHandler countingAbortPolicy = (task, rejectedExecutor) -> {
            rejectedCounter.increment();
            abortPolicy.rejectedExecution(task, rejectedExecutor);
        };
        ThreadPoolExecutor rawExecutor = new ThreadPoolExecutor(
                threadPool.poolSize(),
                threadPool.poolSize(),
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(threadPool.queueCapacity()),
                Thread.ofPlatform()
                        .name("notification-timeout-", 1)
                        .daemon(true)
                        .factory(),
                countingAbortPolicy);
        return ExecutorServiceMetrics.monitor(
                meterRegistry,
                rawExecutor,
                "notificationTimeoutExecutor");
    }

    @Bean
    @Order(1)
    NotificationSender kakaoNotificationSender(AlimtalkNotificationProperties props,
                                               @Qualifier("alimtalkRestClient") RestClient alimtalkRestClient,
                                               @Qualifier("alimtalkNotificationCircuitBreaker") CircuitBreaker circuitBreaker,
                                               @Qualifier("notificationTimeLimiter") TimeLimiter notificationTimeLimiter,
                                               @Qualifier("notificationTimeoutExecutor") ExecutorService notificationTimeoutExecutor,
                                               NotificationResilienceProperties resilience) {
        validateTimeoutHierarchy(resilience, props);
        NhnAlimtalkSender raw = new NhnAlimtalkSender(props, alimtalkRestClient);
        return new ResilientNotificationSender(raw, circuitBreaker, notificationTimeLimiter,
                notificationTimeoutExecutor, resilience.timeoutMillis());
    }

    @Bean
    @Order(2)
    NotificationSender smsNotificationSender(SmsNotificationProperties props,
                                             @Qualifier("smsRestClient") RestClient smsRestClient,
                                             @Qualifier("smsNotificationCircuitBreaker") CircuitBreaker circuitBreaker,
                                             @Qualifier("notificationTimeLimiter") TimeLimiter notificationTimeLimiter,
                                             @Qualifier("notificationTimeoutExecutor") ExecutorService notificationTimeoutExecutor,
                                             NotificationResilienceProperties resilience) {
        validateTimeoutHierarchy(resilience, props);
        RealSmsSender raw = new RealSmsSender(props, smsRestClient);
        return new ResilientNotificationSender(raw, circuitBreaker, notificationTimeLimiter,
                notificationTimeoutExecutor, resilience.timeoutMillis());
    }

    @Bean
    PhoneVerificationSender phoneVerificationSender(SmsNotificationProperties props,
                                                     @Qualifier("smsRestClient") RestClient smsRestClient,
                                                     @Qualifier("smsNotificationCircuitBreaker") CircuitBreaker circuitBreaker,
                                                     @Qualifier("notificationTimeLimiter") TimeLimiter notificationTimeLimiter,
                                                     @Qualifier("notificationTimeoutExecutor") ExecutorService notificationTimeoutExecutor,
                                                     NotificationResilienceProperties resilience) {
        validateTimeoutHierarchy(resilience, props);
        PhoneVerificationSender raw = new RealPhoneVerificationSender(props, smsRestClient);
        return new ResilientPhoneVerificationSender(
                raw,
                circuitBreaker,
                notificationTimeLimiter,
                notificationTimeoutExecutor,
                resilience.timeoutMillis());
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
        return result instanceof Boolean sent && !sent;
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
