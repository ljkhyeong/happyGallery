package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.adapter.out.external.http.PooledHttpClientFactory;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.mail.autoconfigure.MailProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;

/** 선택한 메일 서비스의 자격 증명과 전송 제한만 검증한다. */
@Configuration(proxyBeanMethods = false)
@Profile("prod")
class EmailVerificationTransportConfig {

    private static final String SMTP_AUTH = "mail.smtp.auth";
    private static final String SMTP_CONNECTION_TIMEOUT = "mail.smtp.connectiontimeout";
    private static final String SMTP_READ_TIMEOUT = "mail.smtp.timeout";
    private static final String SMTP_WRITE_TIMEOUT = "mail.smtp.writetimeout";
    private static final String SMTP_STARTTLS_ENABLED = "mail.smtp.starttls.enable";
    private static final String SMTP_STARTTLS_REQUIRED = "mail.smtp.starttls.required";
    private static final String SMTP_CHECK_SERVER_IDENTITY = "mail.smtp.ssl.checkserveridentity";

    @Configuration(proxyBeanMethods = false)
    @Profile("prod")
    @ConditionalOnProperty(prefix = "app.external.email-verification", name = "provider",
            havingValue = "smtp", matchIfMissing = true)
    static class Smtp {
        @Bean
        EmailVerificationTransport smtpEmailVerificationTransport(
                JavaMailSender sender, MailProperties mail, EmailVerificationProperties properties) {
            validateEmailVerificationTransport(properties, mail);
            return new RealEmailVerificationSender(sender, properties);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Profile("prod")
    @ConditionalOnProperty(prefix = "app.external.email-verification", name = "provider", havingValue = "ncp")
    static class Ncp {
        @Bean
        CloseableHttpClient ncpMailHttpClient(PooledHttpClientFactory factory,
                                              NcpMailProperties properties,
                                              EmailVerificationProperties email) {
            Assert.hasText(properties.accessKey(), "네이버 메일 NCP_MAIL_ACCESS_KEY는 필수입니다.");
            Assert.hasText(properties.secretKey(), "네이버 메일 NCP_MAIL_SECRET_KEY는 필수입니다.");
            Duration budget = properties.acquireTimeout().plus(properties.connectTimeout())
                    .plus(properties.timeout());
            Assert.isTrue(email.timeout().compareTo(budget) > 0,
                    "이메일 인증 외부 timeout은 네이버 HTTP timeout 합보다 커야 합니다.");
            return factory.create(properties,
                    builder -> builder.disableAutomaticRetries().disableRedirectHandling());
        }

        @Bean
        RestClient ncpMailRestClient(RestClient.Builder builder,
                                     @Qualifier("ncpMailHttpClient") CloseableHttpClient client) {
            return builder.requestFactory(new HttpComponentsClientHttpRequestFactory(client)).build();
        }

        @Bean
        EmailVerificationTransport ncpEmailVerificationTransport(
                @Qualifier("ncpMailRestClient") RestClient client,
                NcpMailProperties properties, EmailVerificationProperties email, Clock clock) {
            return new NcpEmailVerificationSender(client, properties, email, clock);
        }
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
