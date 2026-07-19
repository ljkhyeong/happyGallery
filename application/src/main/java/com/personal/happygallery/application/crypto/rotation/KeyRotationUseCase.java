package com.personal.happygallery.application.crypto.rotation;

public interface KeyRotationUseCase {

    RotationResult rotate(String sourceKeyId);

    record RotationResult(int users, int guests, int paymentAttempts,
                          int fulfillments, int socialAccounts,
                          int deletedPhoneVerifications, long pendingSocialAccounts) {}
}
