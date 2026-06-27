package com.personal.happygallery.application.token;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;

public final class TokenSigningException extends HappyGalleryException {

    public TokenSigningException() {
        super(ErrorCode.INTERNAL_ERROR, "토큰 서명에 실패했습니다.");
    }
}
