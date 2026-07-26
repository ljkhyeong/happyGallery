package com.personal.happygallery.application.crypto.rotation;

public interface KeyRotationUseCase {

    RotationResult rotate(String sourceKeyId);

    record RotationResult(int users, int guests, int bookings, int paymentAttempts,
                          int fulfillments, int socialAccounts, int adminMfaSecrets,
                          int deletedPhoneVerifications, long pendingSocialAccounts,
                          long pendingAdminMfaSecrets) {}
}
