package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.adapter.out.external.http.ExternalBaseUrl;
import com.personal.happygallery.adapter.out.external.http.HttpPoolProperties;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.external.alimtalk")
public record AlimtalkNotificationProperties(
        @NotBlank String appKey,
        @NotBlank String secretKey,
        @NotBlank @Size(max = 40) String senderKey,
        @NotBlank @DefaultValue("https://kakaotalk-bizmessage.api.nhncloudservice.com") String baseUrl,
        @Min(1) @DefaultValue("2000") long timeoutMillis,
        @Min(1) @DefaultValue("1000") long connectTimeoutMillis,
        @Min(1) @DefaultValue("500") long acquireTimeoutMillis,
        @Min(1) @DefaultValue("20") int maxConnections,
        @Min(1) @DefaultValue("30000") long keepAliveMillis
) implements HttpPoolProperties {

    public AlimtalkNotificationProperties {
        ExternalBaseUrl.requireHttpsOrigin(baseUrl, "알림톡");
    }
}
