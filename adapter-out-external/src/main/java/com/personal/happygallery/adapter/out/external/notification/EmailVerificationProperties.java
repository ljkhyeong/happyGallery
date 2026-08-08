package com.personal.happygallery.adapter.out.external.notification;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.external.email-verification")
public record EmailVerificationProperties(
        @Email String from,
        @NotBlank @DefaultValue("[해피갤러리] 이메일 인증번호") String subject,
        @NotNull @DurationMin(millis = 1) @DefaultValue("7s") Duration timeout
) {}
