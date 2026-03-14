package com.personal.happygallery.config.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "app.admin")
public class AdminProperties {

    @NotBlank
    private String apiKey = "dev-admin-key";

    private boolean enableApiKeyAuth = true;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public boolean isEnableApiKeyAuth() {
        return enableApiKeyAuth;
    }

    public void setEnableApiKeyAuth(boolean enableApiKeyAuth) {
        this.enableApiKeyAuth = enableApiKeyAuth;
    }
}
