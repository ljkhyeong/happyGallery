package com.personal.happygallery.adapter.out.external.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.mail.autoconfigure.MailSenderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.assertj.core.api.Assertions.assertThat;

class SpringMailAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MailSenderAutoConfiguration.class))
            .withInitializer(context -> context.getEnvironment().setActiveProfiles("prod"))
            .withUserConfiguration(ProdMailConsumerConfig.class)
            .withPropertyValues(
                    "spring.mail.host=smtp.example.com",
                    "spring.mail.port=587",
                    "spring.mail.username=smtp-user",
                    "spring.mail.password=smtp-password",
                    "spring.mail.ssl.enabled=false",
                    "spring.mail.ssl.verify-hostname=true",
                    "spring.mail.properties[mail.smtp.auth]=true",
                    "spring.mail.properties[mail.smtp.connectiontimeout]=1000",
                    "spring.mail.properties[mail.smtp.timeout]=2000",
                    "spring.mail.properties[mail.smtp.writetimeout]=2000",
                    "spring.mail.properties[mail.smtp.starttls.enable]=true",
                    "spring.mail.properties[mail.smtp.starttls.required]=true",
                    "spring.mail.properties[mail.smtp.ssl.checkserveridentity]=true");

    @DisplayName("운영 프로필은 Spring Boot가 자동 구성한 JavaMailSender를 주입받는다")
    @Test
    void prodProfile_injectsAutoConfiguredJavaMailSender() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(JavaMailSender.class);

            JavaMailSender mailSender = context.getBean(JavaMailSender.class);
            JavaMailSenderImpl implementation = (JavaMailSenderImpl) mailSender;

            assertThat(context.getBean(ProdMailConsumer.class).mailSender()).isSameAs(mailSender);
            assertThat(implementation.getHost()).isEqualTo("smtp.example.com");
            assertThat(implementation.getJavaMailProperties())
                    .containsEntry("mail.smtp.starttls.required", "true")
                    .containsEntry("mail.smtp.ssl.checkserveridentity", "true");
        });
    }

    @Configuration(proxyBeanMethods = false)
    @Profile("prod")
    static class ProdMailConsumerConfig {

        @Bean
        ProdMailConsumer prodMailConsumer(JavaMailSender mailSender) {
            return new ProdMailConsumer(mailSender);
        }
    }

    private record ProdMailConsumer(JavaMailSender mailSender) {}
}
