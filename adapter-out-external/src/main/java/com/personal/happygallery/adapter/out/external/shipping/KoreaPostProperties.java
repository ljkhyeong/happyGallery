package com.personal.happygallery.adapter.out.external.shipping;

import com.personal.happygallery.adapter.out.external.http.HttpPool;
import jakarta.validation.Valid;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.external.korea-post")
public record KoreaPostProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("") String serviceKey,
        @Valid @DefaultValue HttpPool http
) {
    public KoreaPostProperties {
        if (enabled && !StringUtils.hasText(serviceKey)) {
            throw new IllegalArgumentException("우체국 배송조회를 사용하려면 공공데이터포털 서비스키가 필요합니다.");
        }
    }
}
