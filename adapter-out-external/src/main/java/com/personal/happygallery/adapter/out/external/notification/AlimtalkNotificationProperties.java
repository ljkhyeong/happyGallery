package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.adapter.out.external.http.ExternalBaseUrl;
import com.personal.happygallery.adapter.out.external.http.HttpPool;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.external.alimtalk")
public record AlimtalkNotificationProperties(
        String appKey,
        String secretKey,
        @Size(max = 40) String senderKey,
        @NotBlank @DefaultValue("https://kakaotalk-bizmessage.api.nhncloudservice.com") String baseUrl,
        @Valid @DefaultValue HttpPool http
) {

    public AlimtalkNotificationProperties {
        ExternalBaseUrl.requireHttpsOrigin(baseUrl, "알림톡");
    }
}
