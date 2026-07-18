package com.personal.happygallery.adapter.in.web.ratelimit;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;

public final class RateLimitUnavailableException extends HappyGalleryException {

    public RateLimitUnavailableException() {
        super(ErrorCode.SERVICE_UNAVAILABLE);
    }
}
