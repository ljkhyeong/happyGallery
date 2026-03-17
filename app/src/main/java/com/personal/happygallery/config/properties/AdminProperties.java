package com.personal.happygallery.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.admin")
public class AdminProperties {

    private String apiKey = "";

    private boolean enableApiKeyAuth = false;

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
