package com.personal.happygallery.adapter.out.external.notification;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.web.client.RestClient;

/**
 * 카카오 알림톡 / NHN SMS 어댑터를 {@link ResilientNotificationSender}로 감싸 등록한다.
 *
 * <p>raw sender는 컨텍스트에 빈으로 노출하지 않고 데코레이터만 노출해야
 * {@code NotificationService}의 채널 fallback 체인에 같은 채널이 두 번 들어가지 않는다.
 */
@Configuration
@Profile("prod")
class NotificationResilienceConfig {

    private static final AtomicInteger THREAD_SEQ = new AtomicInteger(0);

    @Bean
    CircuitBreaker kakaoNotificationCircuitBreaker(NotificationResilienceProperties properties) {
        return CircuitBreaker.of("kakaoNotification", circuitBreakerConfig(properties.circuitBreaker()));
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
        ExecutorService rawExecutor = Executors.newFixedThreadPool(
                Math.max(2, properties.circuitBreaker().permittedCallsInHalfOpenState() * 2),
                runnable -> {
                    Thread thread = new Thread(runnable);
                    thread.setName("notification-timeout-" + THREAD_SEQ.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                });
        return ExecutorServiceMetrics.monitor(
                meterRegistry,
                rawExecutor,
                "executor",
                Tags.of("name", "notificationTimeoutExecutor"));
    }

    @Bean
    @Order(1)
    NotificationSender kakaoNotificationSender(KakaoNotificationProperties props,
                                               @Qualifier("kakaoRestClient") RestClient kakaoRestClient,
                                               @Qualifier("kakaoNotificationCircuitBreaker") CircuitBreaker circuitBreaker,
                                               TimeLimiter notificationTimeLimiter,
                                               ExecutorService notificationTimeoutExecutor,
                                               NotificationResilienceProperties resilience) {
        KakaoAlimtalkSender raw = new KakaoAlimtalkSender(props, kakaoRestClient, new KakaoTemplateCatalog());
        return new ResilientNotificationSender(raw, circuitBreaker, notificationTimeLimiter,
                notificationTimeoutExecutor, resilience.timeoutMillis());
    }

    @Bean
    @Order(2)
    NotificationSender smsNotificationSender(SmsNotificationProperties props,
                                             @Qualifier("smsRestClient") RestClient smsRestClient,
                                             @Qualifier("smsNotificationCircuitBreaker") CircuitBreaker circuitBreaker,
                                             TimeLimiter notificationTimeLimiter,
                                             ExecutorService notificationTimeoutExecutor,
                                             NotificationResilienceProperties resilience) {
        RealSmsSender raw = new RealSmsSender(props, smsRestClient, new SmsMessageCatalog());
        return new ResilientNotificationSender(raw, circuitBreaker, notificationTimeLimiter,
                notificationTimeoutExecutor, resilience.timeoutMillis());
    }

    private static CircuitBreakerConfig circuitBreakerConfig(NotificationResilienceProperties.CircuitBreaker cb) {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(cb.failureRateThreshold())
                .slidingWindowSize(cb.slidingWindowSize())
                .minimumNumberOfCalls(cb.minimumNumberOfCalls())
                .waitDurationInOpenState(Duration.ofSeconds(cb.waitDurationOpenSeconds()))
                .permittedNumberOfCallsInHalfOpenState(cb.permittedCallsInHalfOpenState())
                .build();
    }
}
