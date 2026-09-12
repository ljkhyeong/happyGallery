package com.personal.happygallery.application.security.port.out;

public interface BotChallengeVerifier {
    String siteKey();

    boolean verify(String token, String action);
}
