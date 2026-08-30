package com.personal.happygallery.application.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.payment")
public record PublicPaymentProperties(boolean requireCompleteBusinessProfile) {}
