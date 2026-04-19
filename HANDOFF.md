# HANDOFF

이 파일은 다음 **AI 에이전트용 인수인계 문서**다.  
사람용 긴 변경 이력은 두지 않는다. 현재 상태, 우선순위, 작업 규칙만 짧게 유지한다.

## 이 파일의 목적

- 다음 AI 에이전트가 세션 시작 직후 가장 먼저 읽는 문서다.
- 현재 코드 기준으로 바로 필요한 사실만 남긴다.
- 오래된 작업 이력, 세부 변경 로그, 이미 문서화된 설계 배경은 넣지 않는다.

## 우선 확인 문서

1. `README.md`
2. `plan.md`
3. 관련 `docs/PRD`
4. 관련 `docs/ADR`
5. 필요할 때만 `docs/Idea`

원칙:
- 현재 동작과 계약은 `README.md`, `docs/PRD`, `docs/ADR`를 우선한다.
- `docs/Idea`는 배경 메모다. 구현 기준 문서가 아니다.

## 현재 상태

- 현재 브랜치: `payment-integration`
- 운영 주소: `https://d36l7yi27358tl.cloudfront.net/`
- 백엔드는 6개 모듈 구조다.
  - `bootstrap`
  - `adapter-in-web`
  - `adapter-out-persistence`
  - `adapter-out-external`
  - `application`
  - `domain`
- 프론트는 `frontend/`, 운영 모니터링 설정은 `monitoring/`에 있다.

## 현재 워크트리 메모

- 워크트리가 깨끗하지 않다.
- 현재 수정 파일은 주로 문서와 `.github/workflows/deploy.yml`이다.
- 코드 쪽에는 untracked 파일 `application/src/main/java/com/personal/happygallery/application/payment/port/out/PaymentConfirmResult.java`가 있다.
- 작업 시작 전 `git status --short`로 다시 확인하고, 남의 변경은 되돌리지 않는다.

## 현재 운영 기준

- 인증 방식
  - 회원: `HG_SESSION`
  - 관리자: Bearer 세션
  - 비회원: `X-Access-Token`
- 주요 경로
  - 스토어: `/products`
  - 예약 생성: `/bookings/new`
  - 8회권 구매: `/passes/purchase`
  - 비회원 조회: `/guest`
  - 관리자: `/admin`
- 비회원 경로는 현재도 유지 중이지만, 운영상으로는 “보조 경로” 취급이다.
- 운영 배포 구조는 `CloudFront + S3 + ALB + ECS Fargate + RDS + ElastiCache Redis`다.
- `local`에서는 기본 관리자 `admin / admin1234`가 자동 생성된다.
- `local`이 아닌 환경에서는 `ADMIN_SETUP_TOKEN`으로 `/api/v1/admin/setup`을 통해 최초 관리자 계정을 만든다.

## 현재 활성 목표

`plan.md` 기준 큰 정리 작업은 대부분 완료 상태다.  
지금 바로 잡아야 할 우선순위는 아래 순서로 본다.

1. 운영에 직접 영향을 주는 보안/오동작
2. 실제 사용자 경로 회귀
3. 테스트 공백
4. 구조와 문서의 불일치

즉시 진행 후보:
- 관측성 대시보드/알림 규칙 추가 보강
- `/guest` 보조 경로 유지 여부 검토
- 구현과 문서 차이 재정리

## 작업 규칙

- 문서를 수정하면 `README.md`, `HANDOFF.md`, 관련 `docs/PRD`, `docs/ADR`까지 같이 맞춘다.
- 구현 변경 시 가장 작은 관련 테스트부터 실행한다.
- Testcontainers 계열은 기본적으로 `./gradlew --no-daemon ...`를 쓴다.
- 오래된 표현을 문서에 남기지 말고, 사용자 기준 표현과 실제 코드 기준 표현을 우선한다.
- 리팩토링 전에는 `rg`로 같은 패턴이 다른 곳에도 있는지 먼저 확인한다.

## 자주 쓰는 명령

```bash
./gradlew build
./gradlew test
./gradlew :bootstrap:bootRun
./gradlew :application:policyTest
./gradlew --no-daemon :application:useCaseTest
docker compose up -d
```

## 빠른 판단 기준

- 제품 요구사항이 궁금하면 `docs/PRD/0001_기준_스펙/spec.md`
- API 계약이 궁금하면 `docs/PRD/0004_API_계약/spec.md`
- 설계 이유가 궁금하면 관련 `docs/ADR`
- 배포 구조가 궁금하면 `README.md`, `docs/Idea/0028_*`, `0029_*`, `0039_*`

## 이 파일 유지 규칙

- 다음 AI 에이전트가 바로 행동할 수 있는 정보만 남긴다.
- 이미 `README.md`, `plan.md`, `docs/PRD`, `docs/ADR`에 있는 긴 설명은 복붙하지 않는다.
- 길어지면 줄인다. 역사 기록 대신 현재 상태를 우선한다.
