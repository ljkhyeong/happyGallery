package com.personal.happygallery.adapter.in.web.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 최초 관리자 계정 one-time setup 용 토큰.
 * <p>토큰이 비어 있으면 setup 엔드포인트가 비활성화된다.
 * 운영자는 배포 직후 {@code ADMIN_SETUP_TOKEN} env 를 잠깐 세팅하여 setup 을 완료한 뒤 제거한다.
 */
@ConfigurationProperties(prefix = "app.admin.setup")
public record AdminSetupProperties(
        @DefaultValue("") String token
) {
    public boolean enabled() {
        return token != null && !token.isBlank();
    }
}
