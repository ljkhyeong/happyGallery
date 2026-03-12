package com.personal.happygallery.infra.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.external.payment")
public class ExternalPaymentProperties {

    private long timeoutMillis = 3000;
    private CircuitBreaker circuitBreaker = new CircuitBreaker();

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }

    public void setCircuitBreaker(CircuitBreaker circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
    }

    public static class CircuitBreaker {

        private float failureRateThreshold = 50;
        private int slidingWindowSize = 20;
        private int minimumNumberOfCalls = 10;
        private long waitDurationOpenSeconds = 30;
        private int permittedCallsInHalfOpenState = 3;

        public float getFailureRateThreshold() {
            return failureRateThreshold;
        }

        public void setFailureRateThreshold(float failureRateThreshold) {
            this.failureRateThreshold = failureRateThreshold;
        }

        public int getSlidingWindowSize() {
            return slidingWindowSize;
        }

        public void setSlidingWindowSize(int slidingWindowSize) {
            this.slidingWindowSize = slidingWindowSize;
        }

        public int getMinimumNumberOfCalls() {
            return minimumNumberOfCalls;
        }

        public void setMinimumNumberOfCalls(int minimumNumberOfCalls) {
            this.minimumNumberOfCalls = minimumNumberOfCalls;
        }

        public long getWaitDurationOpenSeconds() {
            return waitDurationOpenSeconds;
        }

        public void setWaitDurationOpenSeconds(long waitDurationOpenSeconds) {
            this.waitDurationOpenSeconds = waitDurationOpenSeconds;
        }

        public int getPermittedCallsInHalfOpenState() {
            return permittedCallsInHalfOpenState;
        }

        public void setPermittedCallsInHalfOpenState(int permittedCallsInHalfOpenState) {
            this.permittedCallsInHalfOpenState = permittedCallsInHalfOpenState;
        }
    }
}
