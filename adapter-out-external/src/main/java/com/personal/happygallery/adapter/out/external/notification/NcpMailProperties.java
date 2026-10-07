package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.adapter.out.external.http.HttpPool;
import jakarta.validation.Valid;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.external.ncp-mail")
public record NcpMailProperties(
        String accessKey,
        String secretKey,
        @Valid @DefaultValue HttpPool http
) {
    @Override
    public String toString() {
        return "NcpMailProperties[credentials=redacted]";
    }
}
