# 6개 모듈 분리와 의존 방향 정리 메모

> **구현 완료** — 현재 백엔드는 `bootstrap`, `adapter-in-web`, `adapter-out-persistence`, `adapter-out-external`, `application`, `domain` 6개 모듈 구조를 사용한다. 이 문서는 구조 정리 배경을 남기는 메모다.

## 변경 전

초기 구조는 `app / domain / infra / common` 기준이었다.

이 구조는 시작은 빠르지만, 시간이 지나면서 아래 문제가 커졌다.

- 웹 진입점과 업무 로직, 인프라 구현의 경계가 흐려진다.
- `app`이 `infra` 구현을 바로 알게 된다.
- 테스트와 문서가 현재 유스케이스 경계를 설명하기 어려워진다.

## 변경 후

현재 구조는 아래와 같다.

```text
bootstrap
  -> adapter-in-web
  -> adapter-out-persistence
  -> adapter-out-external
  -> application
  -> domain
```

의존 방향은 `bootstrap -> adapter -> application -> domain` 으로 고정한다.

### 각 모듈의 역할

- `bootstrap`: 앱 시작점과 공통 설정
- `adapter-in-web`: 컨트롤러, 필터, 요청/응답 처리
- `adapter-out-persistence`: JPA, MyBatis, DB 접근
- `adapter-out-external`: 결제, 알림, OAuth, Redis 세션, 외부 HTTP
- `application`: 유스케이스, 포트, 업무 흐름, 배치
- `domain`: 핵심 도메인 규칙

## 이번 정리에서 바뀐 점

- 기존 `app`은 `application`과 `adapter-in-web` 책임으로 나눴다.
- 기존 `infra`는 `adapter-out-persistence`와 `adapter-out-external`로 나눴다.
- 공통 테스트 인프라는 `application/src/testFixtures/**`로 옮겼다.
- 실행 산출물은 `:bootstrap:bootJar` 하나로 통일했다.

## 지금도 유지하는 원칙

- `application`은 `UseCase`, `Port` 같은 경계를 정의한다.
- 웹과 배치는 유스케이스만 호출한다.
- 영속성과 외부 연동 구현은 각 어댑터 모듈에 둔다.
- 모듈 경계는 `LayerDependencyArchTest`로 검증한다.

## 이번 범위에 넣지 않은 것

도메인 객체와 JPA 엔티티를 완전히 분리하는 작업은 이번 구조 정리 범위에 넣지 않았다.

이유:

- 파일 수와 매핑 코드가 크게 늘어난다.
- 현재 도메인 모델과 DB 스키마가 대부분 1:1에 가깝다.
- 지금 단계에서는 경계 정리 효과보다 비용이 더 크다.
