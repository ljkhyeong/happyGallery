# 캐시 스탬피드(Thundering Herd) 방어 전략

**날짜**: 2026-03-24
**상태**: 검토 전

---

## 배경

현재 Redis 사용처는 세션 저장소(`AdminSessionStore`, Spring Session)와 Rate Limit 카운터(`RateLimitFilter`)뿐이다.
조회 결과 캐싱(`@Cacheable`, `RedisCacheManager`)은 아직 도입되지 않았으므로 캐시 스탬피드가 발생할 구조가 없다.

향후 트래픽이 증가하여 **상품 목록, 공지사항, 슬롯 현황** 등을 Redis에 캐싱할 경우,
고정 TTL 만료 시점에 다수 요청이 동시에 DB를 조회하는 캐시 스탬피드(Thundering Herd)가 발생할 수 있다.

---

## 문제 시나리오

```
TTL 만료 (T=3600s)
  ↓
1만 요청 동시 도착 → 캐시 미스 → 1만 SELECT 쿼리 → DB 과부하
```

---

## 방어 전략

### 1. TTL 지터(Jitter)

만료 시간을 분산시켜 동시 만료를 방지한다.

```java
Duration baseTtl = Duration.ofHours(1);
Duration jitter = Duration.ofSeconds(ThreadLocalRandom.current().nextLong(0, 300));
Duration effectiveTtl = baseTtl.plus(jitter);
```

- 장점: 구현이 단순하고 부작용이 없다
- 한계: 동일 키에 대한 스탬피드는 방어 불가 (키별 만료 시점이 하나이므로)

### 2. 분산 락(Distributed Lock) — Redisson `RLock`

캐시 미스 시 DB 조회 권한을 1개 스레드에만 부여하고, 나머지는 대기 후 갱신된 캐시를 읽는다.

```java
RLock lock = redissonClient.getLock("cache:rebuild:" + cacheKey);
if (lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS)) {
    try {
        // 이중 체크: 락 획득 사이에 다른 스레드가 이미 갱신했을 수 있음
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) return cached;

        String fresh = loadFromDb();
        redisTemplate.opsForValue().set(cacheKey, fresh, effectiveTtl);
        return fresh;
    } finally {
        lock.unlock();
    }
}
```

- 장점: 동일 키 스탬피드를 완전 차단
- 주의: Redisson 의존성 추가 필요, 락 leaseTime 설정 필수 (데드락 방지)

### 3. PER(Probabilistic Early Recomputation)

TTL 만료 전에 확률적으로 미리 캐시를 갱신한다.

```
만료까지 남은 시간이 짧을수록 재계산 확률이 높아짐
→ TTL 만료 시점에는 이미 캐시가 갱신되어 스탬피드 없음
```

- 장점: 락 없이 방어 가능, 지연 시간 균일
- 한계: 구현 복잡도가 높고, 불필요한 조기 갱신이 발생할 수 있음

---

## 권장 조합

| 단계 | 전략 | 적용 시점 |
|------|------|----------|
| 1단계 | TTL 지터 | `@Cacheable` 도입 시 기본 적용 |
| 2단계 | 분산 락 | 핫 키(상품 상세, 메인 배너) 대상 선별 적용 |
| 3단계 | PER | 트래픽 규모가 커져 락 경합이 병목이 될 때 검토 |

---

## 현재 상태와 선행 조건

- [ ] 조회 캐싱 대상 식별 (상품 목록, 슬롯 현황, 공지사항 등)
- [ ] `RedisCacheManager` + `@Cacheable` 기본 구조 도입
- [ ] Redisson 의존성 추가 여부 결정
- [ ] 캐시 히트율 모니터링 메트릭 설계 (`cache.hit`, `cache.miss`)

---

## 참고

- [Idea-0015 다중 인스턴스용 Redis 도입](../0015_다중_인스턴스용_Redis_도입/idea.md) — Redis 인프라 기반
- [ADR-0017 Filter 처리율 제한](../../ADR/0017_Filter_처리율_제한/adr.md) — 현재 Redis Lua 스크립트 활용 사례
