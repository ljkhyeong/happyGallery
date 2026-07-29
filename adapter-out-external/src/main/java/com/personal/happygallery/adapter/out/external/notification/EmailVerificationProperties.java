package com.personal.happygallery.adapter.out.external.notification;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.external.email-verification")
public record EmailVerificationProperties(
        String host,
        @Min(1) @DefaultValue("587") int port,
        String username,
        String password,
        @Email String from,
        @NotBlank @DefaultValue("[해피갤러리] 이메일 인증번호") String subject,
        @Min(1) @DefaultValue("7000") long timeoutMillis,
        @Min(1) @DefaultValue("1000") int connectionTimeoutMillis,
        @Min(1) @DefaultValue("2000") int readTimeoutMillis,
        @Min(1) @DefaultValue("2000") int writeTimeoutMillis,
        @DefaultValue("true") boolean startTlsEnabled,
        @DefaultValue("false") boolean sslEnabled
) {}
