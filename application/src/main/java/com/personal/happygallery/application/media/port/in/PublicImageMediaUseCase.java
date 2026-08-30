package com.personal.happygallery.application.media.port.in;

/** 인증 없는 공개 이미지 경로에 적용할 노출 정책을 포함한 조회 포트. */
public interface PublicImageMediaUseCase {

    ImageMediaUseCase.ImageContent get(String fileName);
}
