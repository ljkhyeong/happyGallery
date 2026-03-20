# Redis 도입 — 다중 인스턴스 대응

**날짜**: 2026-03-18
**상태**: ADR 반영 완료

> 최종 채택 기준은 [ADR-0023](../../ADR/0023_admin-auth-and-runtime-operations-baseline/adr.md), [ADR-0017](../../ADR/0017_filter-rate-limiting/adr.md) 확인. 이 문서는 전환 배경과 검토 맥락 보존용이다.

---

## 배경

다중 인스턴스 배포(이중화)를 위해 현재 JVM 힙에 상태를 저장하는 두 컴포넌트를 검토했다.

- **AdminSessionStore**: `ConcurrentHashMap` 기반 인메모리 → 인스턴스 A에서 로그인한 관리자 세션이 인스턴스 B에서 인식되지 않음
- **RateLimitFilter**: Bucket4j 인메모리 bucket → 인스턴스별 rate limit 카운터가 분리되어 다중 인스턴스 환경에서 제한 우회 가능

회원 세션도 이번에 Spring Session + Redis로 전환해 다중 인스턴스에서 같은 세션 저장소를 공유하도록 맞췄다.

---

## 적용 내용

### Member Session → Spring Session + Redis

회원 세션의 직접 구현을 제거하고 Spring Session + Redis로 전환했다.

- 쿠키 이름: `HG_SESSION` 유지
- 세션 attribute: `customerUserId`
- `UserSession`, `UserSessionRepository`, `CustomerSessionPort*` 제거
- `CustomerAuthController`는 로그인/회원가입 시 `HttpSession`에 사용자 ID를 기록하고, 로그아웃은 세션 무효화로 처리
- `CustomerAuthFilter`는 Spring Session filter 이후 실행되며 세션 사용자 ID로 회원 정보를 다시 조회

### AdminSession → Redis

`AdminSessionStore`를 `StringRedisTemplate` 기반으로 전환했다.

- 키 패턴: `admin:session:{token}`
- TTL: 8시간 (Redis EXPIRE로 자동 처리 — 기존 수동 만료 체크 제거)
- 직렬화: Jackson ObjectMapper로 JSON 변환

기존 `AdminSessionPort` 인터페이스가 이미 추상화 경계를 제공하므로 구현 교체만으로 변경이 완료되었다.

### RateLimitFilter → Redis 공유 카운터

Bucket4j 인메모리 bucket 대신 Redis 공유 카운터로 전환했다.

```
INCR rate:{RULE_ID}:{clientIP}
EXPIRE rate:{RULE_ID}:{clientIP} {window_seconds}  ← count == 1일 때만 TTL 설정
```

- 인스턴스 간 카운터가 공유되어 정확한 rate limit 적용
- Redis TTL이 윈도우 만료를 자동 처리 → `@Scheduled evictStaleBuckets()` 제거
- `TimestampedBucket` inner class 제거
- 현재 구현은 Lua script로 `INCR`와 최초 `EXPIRE`를 한 번에 실행한다.

---

## 인프라 변경

### docker-compose.yml

```yaml
redis:
  image: redis:7-alpine
  healthcheck:
    test: ["CMD", "redis-cli", "ping"]
```

앱 서비스에 `REDIS_HOST: redis` 환경변수 추가, `depends_on: redis` 추가.

### application.yml

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
```

### 테스트 (Testcontainers)

`TestcontainersConfig`에 Redis 컨테이너 추가. MySQL과 동일한 `@ServiceConnection` 패턴 적용.

```java
@Bean
@ServiceConnection(name = "redis")
GenericContainer<?> redisContainer() {
    return new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);
}
```

---

## 비적용 범위

| 항목 | 이유 |
|------|------|
| 관리자/회원 세션 통합 인증 프레임워크 전환 | 현재는 관리자 Bearer 세션과 회원 Spring Session을 분리 유지하는 편이 변경 범위를 더 작게 만든다 |

---

## 의존성

- `spring-boot-starter-data-redis` (BOM 버전 관리, Lettuce 포함)
- `testcontainers:testcontainers` (BOM 버전 관리, Redis 컨테이너용)
