package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.adapter.out.external.http.ExternalBaseUrl;
import com.personal.happygallery.adapter.out.external.http.HttpPool;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.external.sms")
public record SmsNotificationProperties(
        String apiKey,
        String apiSecret,
        String senderNumber,
        @NotBlank @DefaultValue("https://sms.api.nhncloudservice.com") String baseUrl,
        @Valid @DefaultValue HttpPool http
) {

    public SmsNotificationProperties {
        ExternalBaseUrl.requireHttpsOrigin(baseUrl, "SMS");
    }
}
