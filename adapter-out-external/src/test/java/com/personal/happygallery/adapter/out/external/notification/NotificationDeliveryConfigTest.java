package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.adapter.out.external.http.PooledHttpClientFactory;
import com.personal.happygallery.adapter.out.external.resilience.BoundedExecutorFactory;
import com.personal.happygallery.application.customer.port.out.EmailVerificationSender;
import com.personal.happygallery.application.customer.port.out.PhoneVerificationSender;
import com.personal.happygallery.application.notification.port.out.NotificationDeliveryResultProvider;
import com.personal.happygallery.application.notification.port.out.NotificationSendResult;
import com.personal.happygallery.application.notification.port.out.NotificationSenderPort;
import com.personal.happygallery.domain.notification.NotificationChannel;
import com.personal.happygallery.domain.notification.NotificationEventType;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.mail.autoconfigure.MailProperties;
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationDeliveryConfigTest {
    private final JavaMailSender smtp = mock();
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(NotificationDeliveryConfig.class, NotificationResilienceConfig.class,
                    NotificationRestClientConfig.class, EmailVerificationTransportConfig.class, PropertiesConfig.class)
            .withBean(PooledHttpClientFactory.class)
            .withBean(RestClient.Builder.class, RestClient::builder)
            .withBean(Clock.class, Clock::systemUTC)
            .withBean(CircuitBreakerRegistry.class, CircuitBreakerRegistry::ofDefaults)
            .withBean(BoundedExecutorFactory.class, () -> new BoundedExecutorFactory(
                    new ThreadPoolTaskExecutorBuilder(), new SimpleMeterRegistry(), task -> task))
            .withBean(JavaMailSender.class, () -> smtp)
            .withPropertyValues("spring.profiles.active=prod",
                    "app.external.notification.alimtalk-thread-pool.pool-size=1",
                    "app.external.notification.sms-thread-pool.pool-size=1",
                    "app.external.notification.phone-verification-thread-pool.pool-size=1",
                    "app.external.notification.email-verification-thread-pool.pool-size=1",
                    "app.external.notification.circuit-breaker.failure-rate-threshold=50",
                    "app.external.email-verification.from=no-reply@mail.happy-gallery.com",
                    "spring.mail.host=smtp.example.com", "spring.mail.port=587",
                    "spring.mail.username=test", "spring.mail.password=test",
                    "spring.mail.properties[mail.smtp.auth]=true",
                    "spring.mail.properties[mail.smtp.starttls.enable]=true",
                    "spring.mail.properties[mail.smtp.starttls.required]=true",
                    "spring.mail.properties[mail.smtp.ssl.checkserveridentity]=true",
                    "spring.mail.properties[mail.smtp.connectiontimeout]=1000",
                    "spring.mail.properties[mail.smtp.timeout]=2000",
                    "spring.mail.properties[mail.smtp.writetimeout]=2000");

    @Test
    @DisplayName("중지 모드는 NHN 키 없이 시작하고 문자 실패와 실제 이메일 전송을 분리한다")
    void disabledStartsWithoutCredentialsAndKeepsEmail() {
        runner.withPropertyValues("app.external.notification.mode=disabled").run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(PhoneVerificationSender.class)
                    .hasSingleBean(EmailVerificationSender.class)
                    .doesNotHaveBean("alimtalkRestClient").doesNotHaveBean("smsRestClient")
                    .doesNotHaveBean(NotificationDeliveryResultProvider.class);
            var senders = context.getBeansOfType(NotificationSenderPort.class).values();
            assertThat(senders).extracting(NotificationSenderPort::channel)
                    .containsExactlyInAnyOrder(NotificationChannel.KAKAO, NotificationChannel.SMS);
            for (var sender : senders) {
                assertThat(sender.send("test", "01012345678", "테스트", NotificationEventType.BOOKING_CONFIRMED))
                        .isEqualTo(NotificationSendResult.PERMANENT_FAILURE);
            }
            assertThat(context.getBean(PhoneVerificationSender.class).send("01012345678", "123456")).isFalse();
            assertThat(context.getBean(EmailVerificationSender.class).send("member@example.com", "654321")).isTrue();
            verify(smtp).send(any(SimpleMailMessage.class));
        });
    }

    @Test
    @DisplayName("모드가 없던 기존 운영 환경은 NHN 전송기와 결과 조회기를 사용한다")
    void defaultUsesNhn() {
        nhnRunner().run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(PhoneVerificationSender.class)
                    .doesNotHaveBean("disabledPhoneVerificationSender");
            assertThat(context.getBeansOfType(NotificationSenderPort.class).values()).hasSize(2)
                    .allSatisfy(sender -> assertThat(sender).isInstanceOf(ResilientNotificationSender.class));
            assertThat(context.getBeansOfType(NotificationDeliveryResultProvider.class)).hasSize(2);
            assertThat(context.getBean(PhoneVerificationSender.class)).isInstanceOf(ResilientPhoneVerificationSender.class);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"alimtalk.app-key", "alimtalk.secret-key", "alimtalk.sender-key",
            "sms.api-key", "sms.api-secret", "sms.sender-number"})
    @DisplayName("NHN 모드는 여섯 필수 자격 증명 중 하나만 비어도 시작하지 않는다")
    void nhnRequiresEveryCredential(String key) {
        nhnRunner().withPropertyValues("app.external.notification.mode=nhn", "app.external." + key + "=")
                .run(context -> assertThat(context).hasFailed());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "fake", "typo", "DISABLED"})
    @DisplayName("잘못되거나 빈 모드로 발송 설정을 우회할 수 없다")
    void invalidModeFails(String mode) {
        nhnRunner().withPropertyValues("app.external.notification.mode=" + mode)
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("문자 중지 모드도 실제 이메일 자격 증명은 필요하다")
    void disabledStillRequiresEmail() {
        runner.withPropertyValues("app.external.notification.mode=disabled", "spring.mail.password=")
                .run(context -> assertThat(context).hasFailed());
    }

    private ApplicationContextRunner nhnRunner() {
        return runner.withPropertyValues("app.external.alimtalk.app-key=test",
                "app.external.alimtalk.secret-key=test", "app.external.alimtalk.sender-key=test",
                "app.external.sms.api-key=test", "app.external.sms.api-secret=test",
                "app.external.sms.sender-number=01012345678");
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({AlimtalkNotificationProperties.class, SmsNotificationProperties.class,
            NotificationResilienceProperties.class, EmailVerificationProperties.class,
            NcpMailProperties.class, MailProperties.class})
    static class PropertiesConfig {}
}
