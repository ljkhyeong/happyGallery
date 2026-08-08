package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.adapter.out.external.http.ExternalBaseUrl;
import com.personal.happygallery.adapter.out.external.http.HttpPoolProperties;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.external.sms")
public record SmsNotificationProperties(
        @NotBlank String apiKey,
        @NotBlank String apiSecret,
        @NotBlank String senderNumber,
        @NotBlank @DefaultValue("https://sms.api.nhncloudservice.com") String baseUrl,
        @NotNull @DurationMin(millis = 1) @DefaultValue("2s") Duration timeout,
        @NotNull @DurationMin(millis = 1) @DefaultValue("1s") Duration connectTimeout,
        @NotNull @DurationMin(millis = 1) @DefaultValue("500ms") Duration acquireTimeout,
        @Min(1) @DefaultValue("20") int maxConnections,
        @NotNull @DurationMin(millis = 1) @DefaultValue("30s") Duration keepAlive
) implements HttpPoolProperties {

    public SmsNotificationProperties {
        ExternalBaseUrl.requireHttpsOrigin(baseUrl, "SMS");
    }
}
