package com.personal.happygallery.adapter.in.web.ratelimit;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;

public final class RateLimitExceededException extends HappyGalleryException {

    private final long limit;
    private final long remaining;
    private final long retryAfterSeconds;

    public RateLimitExceededException(RateLimitDecision decision) {
        super(ErrorCode.TOO_MANY_REQUESTS);
        this.limit = decision.limit();
        this.remaining = decision.remaining();
        this.retryAfterSeconds = Math.max(1, decision.window().toSeconds());
    }

    public long limit() {
        return limit;
    }

    public long remaining() {
        return remaining;
    }

    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
