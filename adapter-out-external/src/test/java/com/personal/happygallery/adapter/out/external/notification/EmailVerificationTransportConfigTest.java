package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.adapter.out.external.http.PooledHttpClientFactory;
import com.sun.net.httpserver.HttpServer;
import com.personal.happygallery.application.customer.port.out.EmailVerificationSender;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.net.InetSocketAddress;
import java.time.Clock;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.mail.autoconfigure.MailProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EmailVerificationTransportConfigTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(EmailVerificationTransportConfig.class, PropertiesConfig.class)
            .withBean(PooledHttpClientFactory.class)
            .withBean(RestClient.Builder.class, RestClient::builder)
            .withBean(Clock.class, Clock::systemUTC)
            .withPropertyValues("spring.profiles.active=prod",
                    "app.external.email-verification.from=no-reply@mail.happy-gallery.com");

    @Test
    @DisplayName("네이버 메일은 SMTP 빈과 계정 없이 선택한 전송기 하나만 생성한다")
    void ncpStartsWithoutSmtp() {
        ncpRunner().run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(EmailVerificationTransport.class)
                    .doesNotHaveBean(JavaMailSender.class);
            assertThat(context.getBean(EmailVerificationTransport.class)).isInstanceOf(NcpEmailVerificationSender.class);
        });
    }

    @Test
    @DisplayName("네이버 메일의 누락된 키와 역전된 타임아웃은 시작 시 거부한다")
    void ncpRejectsInvalidConfiguration() {
        ncpRunner().withPropertyValues("app.external.ncp-mail.secret-key=")
                .run(context -> assertThat(context).hasFailed());
        ncpRunner().withPropertyValues("app.external.email-verification.timeout=3500ms")
                .run(context -> assertThat(context).hasFailed());
        ncpRunner().withPropertyValues("app.external.email-verification.provider=typo")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("기존 설정에 제공자가 없으면 SMTP로 인증 메일을 보낸다")
    void defaultsToSmtp() {
        JavaMailSender smtp = mock();
        runner.withUserConfiguration(SenderConfig.class)
                .withBean(JavaMailSender.class, () -> smtp).withPropertyValues(
                "spring.mail.host=smtp.example.com", "spring.mail.port=587",
                "spring.mail.username=test", "spring.mail.password=test",
                "spring.mail.properties[mail.smtp.auth]=true",
                "spring.mail.properties[mail.smtp.starttls.enable]=true",
                "spring.mail.properties[mail.smtp.starttls.required]=true",
                "spring.mail.properties[mail.smtp.ssl.checkserveridentity]=true",
                "spring.mail.properties[mail.smtp.connectiontimeout]=1000",
                "spring.mail.properties[mail.smtp.timeout]=2000",
                "spring.mail.properties[mail.smtp.writetimeout]=2000"
        ).run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(EmailVerificationTransport.class)
                    .doesNotHaveBean("ncpMailHttpClient").hasSingleBean(EmailVerificationSender.class);
            assertThat(context.getBean(EmailVerificationSender.class).send("member@example.com", "123456")).isTrue();
            var message = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(smtp).send(message.capture());
            assertThat(message.getValue().getText()).contains("123456", "5분");
            assertThat(message.getValue().getTo()).containsExactly("member@example.com");
        });
    }

    @Test
    @DisplayName("local 프로필에서는 외부 메일 전송기를 만들지 않는다")
    void localProfileDoesNotCreateExternalTransport() {
        ncpRunner().withUserConfiguration(EmailVerificationTransportConfig.Ncp.class,
                        EmailVerificationTransportConfig.Smtp.class)
                .withPropertyValues("spring.profiles.active=local")
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(EmailVerificationTransport.class));
    }

    @Test
    @DisplayName("네이버 HTTP 클라이언트는 503 재시도와 다른 주소로의 리다이렉트를 하지 않는다")
    void httpClientDoesNotRetryOrRedirect() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requests.incrementAndGet();
            exchange.getRequestBody().readAllBytes();
            if (exchange.getRequestURI().getPath().equals("/redirect")) {
                exchange.getResponseHeaders().add("Location", "/unexpected");
                exchange.sendResponseHeaders(307, -1);
            } else {
                exchange.getResponseHeaders().add("Retry-After", "0");
                exchange.sendResponseHeaders(503, -1);
            }
            exchange.close();
        });
        server.start();
        try (var client = new EmailVerificationTransportConfig.Ncp().ncpMailHttpClient(
                new PooledHttpClientFactory(), NcpEmailVerificationSenderTest.credentials(),
                NcpEmailVerificationSenderTest.emailProperties())) {
            RestClient rest = RestClient.builder().baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                    .requestFactory(new HttpComponentsClientHttpRequestFactory(client)).build();
            assertThatThrownBy(() -> rest.post().uri("/unavailable").body("test").retrieve().toBodilessEntity())
                    .isInstanceOf(HttpServerErrorException.class);
            assertThat(requests).hasValue(1);
            assertThat(rest.post().uri("/redirect").body("test").retrieve().toBodilessEntity().getStatusCode().value())
                    .isEqualTo(307);
            assertThat(requests).hasValue(2);
        } finally {
            server.stop(0);
        }
    }

    private ApplicationContextRunner ncpRunner() {
        return runner.withPropertyValues("app.external.email-verification.provider=ncp",
                "app.external.ncp-mail.access-key=test-key", "app.external.ncp-mail.secret-key=test-secret");
    }

    @Configuration(proxyBeanMethods = false)
    static class SenderConfig {
        @Bean
        EmailVerificationSender sender(EmailVerificationTransport transport, EmailVerificationProperties properties) {
            var config = new NotificationResilienceConfig();
            return config.emailVerificationSender(properties, transport, CircuitBreaker.ofDefaults("test-email"),
                    config.emailVerificationTimeLimiter(properties), Runnable::run);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({EmailVerificationProperties.class, NcpMailProperties.class, MailProperties.class})
    static class PropertiesConfig {}
}
