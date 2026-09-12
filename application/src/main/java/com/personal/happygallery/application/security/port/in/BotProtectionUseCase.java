package com.personal.happygallery.application.security.port.in;

public interface BotProtectionUseCase {
    String siteKey();

    void verify(String token, String action);
}
