package com.personal.happygallery.application.media.port.out;

/** 로컬 이미지 참조 저장과 고아 이미지 삭제를 직렬화한다. */
public interface ImageMediaReferenceLockPort {

    void lock();
}
