package com.personal.happygallery.adapter.out.external.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.external.notification")
public record NotificationDeliveryProperties(
        @NotBlank @Pattern(regexp = "nhn|disabled") @DefaultValue("nhn") String mode
) {}
