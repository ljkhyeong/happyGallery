package com.personal.happygallery.application.policy;

public record PolicyAcceptance(
        String termsVersion,
        boolean termsAccepted,
        String privacyVersion,
        boolean privacyAccepted
) {}
