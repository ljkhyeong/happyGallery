package com.personal.happygallery.config.properties;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;

    private boolean trustForwardedHeaders = false;

    @Min(1)
    private long phoneVerificationPerSecond = 10;

    @Min(1)
    private long bookingCreatePerMinute = 30;

    @Min(1)
    private long passPurchasePerMinute = 20;

    @Min(1)
    private long customerLoginPerMinute = 10;

    @Min(1)
    private long customerSignupPerMinute = 5;

    @Min(1)
    private long adminLoginPerMinute = 5;

    @Min(1)
    private long adminApiPerMinute = 120;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isTrustForwardedHeaders() {
        return trustForwardedHeaders;
    }

    public void setTrustForwardedHeaders(boolean trustForwardedHeaders) {
        this.trustForwardedHeaders = trustForwardedHeaders;
    }

    public long getPhoneVerificationPerSecond() {
        return phoneVerificationPerSecond;
    }

    public void setPhoneVerificationPerSecond(long phoneVerificationPerSecond) {
        this.phoneVerificationPerSecond = phoneVerificationPerSecond;
    }

    public long getBookingCreatePerMinute() {
        return bookingCreatePerMinute;
    }

    public void setBookingCreatePerMinute(long bookingCreatePerMinute) {
        this.bookingCreatePerMinute = bookingCreatePerMinute;
    }

    public long getPassPurchasePerMinute() {
        return passPurchasePerMinute;
    }

    public void setPassPurchasePerMinute(long passPurchasePerMinute) {
        this.passPurchasePerMinute = passPurchasePerMinute;
    }

    public long getCustomerLoginPerMinute() {
        return customerLoginPerMinute;
    }

    public void setCustomerLoginPerMinute(long customerLoginPerMinute) {
        this.customerLoginPerMinute = customerLoginPerMinute;
    }

    public long getCustomerSignupPerMinute() {
        return customerSignupPerMinute;
    }

    public void setCustomerSignupPerMinute(long customerSignupPerMinute) {
        this.customerSignupPerMinute = customerSignupPerMinute;
    }

    public long getAdminLoginPerMinute() {
        return adminLoginPerMinute;
    }

    public void setAdminLoginPerMinute(long adminLoginPerMinute) {
        this.adminLoginPerMinute = adminLoginPerMinute;
    }

    public long getAdminApiPerMinute() {
        return adminApiPerMinute;
    }

    public void setAdminApiPerMinute(long adminApiPerMinute) {
        this.adminApiPerMinute = adminApiPerMinute;
    }
}
