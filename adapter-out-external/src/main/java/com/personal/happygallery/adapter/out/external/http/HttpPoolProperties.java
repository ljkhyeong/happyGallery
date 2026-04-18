package com.personal.happygallery.adapter.out.external.http;

/**
 * 외부 HTTP 커넥션풀 설정에 필요한 공통 프로퍼티.
 * 서비스별 {@code @ConfigurationProperties} record가 이 인터페이스를 구현한다.
 */
public interface HttpPoolProperties {

    long connectTimeoutMillis();

    long timeoutMillis();

    long acquireTimeoutMillis();

    int maxConnections();

    long keepAliveMillis();
}
