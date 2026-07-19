package com.personal.happygallery.application.admin.port.out;

/** 최초 관리자 생성 요청을 DB 단일 행 잠금으로 직렬화한다. */
public interface AdminSetupLockPort {

    void lock();
}
