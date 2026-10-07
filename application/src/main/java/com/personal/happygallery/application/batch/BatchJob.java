package com.personal.happygallery.application.batch;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 배치 실행 로그와 지표를 남긴다. {@link BatchResult}를 반환하는 메서드에만 적용된다. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BatchJob {
    String id();
    String value();
}
