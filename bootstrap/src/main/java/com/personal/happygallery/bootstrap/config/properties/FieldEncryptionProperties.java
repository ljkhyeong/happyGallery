package com.personal.happygallery.bootstrap.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.field-encryption")
public record FieldEncryptionProperties(
        @Pattern(regexp = KEY_ID_PATTERN) @DefaultValue("v1") String activeKeyId,
        @NotBlank String encryptKey,
        @NotBlank String hmacKey,
        @DefaultValue("") String previousEncryptKeys,
        @DefaultValue("") String previousHmacKeys
) {
    /** 활성·이전 키 ID 형식. deploy/k3s/scripts의 키 회전 스크립트도 같은 패턴을 검사한다. */
    public static final String KEY_ID_PATTERN = "^[A-Za-z0-9_-]{1,32}$";
}
