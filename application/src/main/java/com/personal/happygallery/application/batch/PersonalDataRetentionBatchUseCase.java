package com.personal.happygallery.application.batch;

public interface PersonalDataRetentionBatchUseCase {
    BatchResult cleanUpExpiredSensitiveData();
}
