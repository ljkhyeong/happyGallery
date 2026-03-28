# 0032 — HTTP 캐싱 ETag 고도화

## 현재 상태 (Phase 1 — 완료)

`ShallowEtagHeaderFilter`를 products, classes, notices GET 엔드포인트에 적용.

- 응답 body의 MD5 해시를 `ETag` 헤더로 내려줌
- 클라이언트가 `If-None-Match`를 보내면 body 비교 후 `304 Not Modified` 반환
- **한계**: DB 쿼리와 직렬화는 매번 실행됨 — 네트워크 트래픽만 절감

## Phase 2 — Caffeine 로컬 캐시 + Cache-Control (미적용)

트래픽 증가 시 아래 방식으로 고도화한다.

### 적용 대상

| API | Cache-Control | 캐시 TTL | 무효화 시점 |
|-----|---------------|---------|------------|
| `GET /products` | `max-age=60` | 60초 | 상품 등록/수정/상태 변경 |
| `GET /products/{id}` | `max-age=60` | 60초 | 해당 상품 수정 |
| `GET /classes` | `max-age=300` | 5분 | 클래스 추가/변경 |
| `GET /notices` | `max-age=120` | 2분 | 공지 생성/수정 |

### 구현 방향

1. **Caffeine 캐시** 도입 (`spring-boot-starter-cache` + `caffeine`)
   - 서비스 계층에서 `@Cacheable` / `@CacheEvict` 적용
   - DB 쿼리 자체를 스킵하여 응답 지연과 DB 부하 동시 절감

2. **`Cache-Control` 헤더** 추가
   - 브라우저가 TTL 동안 서버 요청 자체를 하지 않음
   - TTL 만료 후에는 `ETag`로 304 판단

3. **캐시 무효화**
   - 관리자 API에서 상품/클래스/공지 CUD 시 `@CacheEvict` 호출
   - 단일 인스턴스에서는 Caffeine으로 충분
   - 다중 인스턴스 시 Redis pub/sub 기반 무효화 필요 (0015 참조)

### Phase 2 도입 시점 판단 기준

- Prometheus `http_server_requests_seconds` 지표에서 products/classes 응답이 p95 > 200ms
- 또는 CloudWatch에서 아웃바운드 비용이 월 $10 이상일 때
