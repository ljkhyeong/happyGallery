# Idea 0028: 운영 배포에서 `nginx` 대신 CloudFront + S3 + ALB 조합 검토

## 배경

현재 저장소에는 `docker-compose.yml`의 `nginx` 서비스와 `nginx/nginx.conf`가 들어 있다.
이 구성은 단일 서버나 단순 컨테이너 배포에서는 이해하기 쉽지만, AWS 운영 배포 기준으로는 정적 파일 서빙과 API 프록시를 한 컨테이너에 같이 맡길 필요가 없다.

운영 대안으로 다음 구성을 검토한다.

- 프론트 정적 파일: `S3 + CloudFront`
- API 진입점: `ALB`
- 백엔드 앱: private subnet

가능하면 브라우저에는 하나의 도메인만 보이도록 `CloudFront`가 `/api/*` 요청을 `ALB` origin으로 라우팅하고, 나머지 경로는 `S3` origin으로 라우팅하는 구성을 우선 검토한다.

## 현재 구성의 장단점

### As-Is

- `nginx` 컨테이너가 frontend `dist`를 직접 서빙한다.
- 같은 `nginx`가 `/api`를 backend app 컨테이너로 reverse proxy 한다.
- 브라우저 기준으로는 same-origin이므로 CORS를 따로 열지 않아도 된다.

장점:

- 로컬/간단 배포에서 구조를 이해하기 쉽다.
- SPA fallback과 `/api` 프록시를 한 파일에서 관리할 수 있다.

제약:

- 운영에서도 정적 파일 서빙을 앱 배포 단위와 같이 굴리게 된다.
- `nginx` 이미지/설정/배포를 계속 관리해야 한다.
- CDN 캐시, 글로벌 엣지 서빙, 정적 자산 무중단 교체는 별도 인프라 이점을 잘 살리지 못한다.

## 대안

### To-Be

- `CloudFront` distribution 하나를 public 진입점으로 둔다.
- 기본 origin은 private `S3` bucket으로 두고 정적 파일을 서빙한다.
- `/api/*` behavior는 `ALB` origin으로 전달한다.
- `ALB` target은 private subnet의 backend 인스턴스 또는 컨테이너다.
- `S3`는 public website endpoint 대신 `OAC`(Origin Access Control)로 CloudFront만 접근 가능하게 둔다.

## 기대 효과

### 1. 정적 자산 운영 단순화

- JS/CSS/이미지는 `S3 + CloudFront`가 더 자연스럽다.
- 앱 배포와 정적 자산 배포를 분리할 수 있다.
- 정적 파일 rollback과 cache invalidation 전략을 독립적으로 가져갈 수 있다.

### 2. 백엔드 노출 최소화

- backend는 private subnet에 두고 public ingress는 `ALB`까지만 노출할 수 있다.
- 운영상 reverse proxy 컨테이너를 별도로 유지하지 않아도 된다.

### 3. same-origin 유지 가능

- `CloudFront`가 `/api/*`를 `ALB`로 라우팅하면 브라우저 기준으로는 프론트와 API가 같은 origin처럼 보인다.
- 이 경우 현재 코드처럼 별도 CORS 설정 없이 운영할 수 있다.

## 주의할 점

### 1. 프론트/API 도메인을 분리하면 CORS를 다시 봐야 한다

현재 backend에는 별도 CORS 허용 설정이 없다.
개발은 Vite dev proxy로 same-origin처럼 맞추고 있고, 운영도 같은 방식으로 가정하면 문제가 없다.

하지만 `cdn.example.com` 과 `api.example.com` 처럼 완전히 다른 origin으로 나누면:

- backend CORS 허용 정책이 필요하다.
- 쿠키 기반 인증이면 credentials 허용과 allowed origin을 함께 설계해야 한다.
- 현재 `HG_SESSION` 쿠키의 `SameSite=Lax` 정책은 cross-site 인증 흐름과 바로 맞지 않을 수 있다.

### 2. SPA fallback 처리는 CloudFront 쪽에서 다시 설계해야 한다

`nginx try_files`를 없애면, 존재하지 않는 프론트 라우트 요청을 `index.html`로 돌리는 정책을 CloudFront/S3 기준으로 다시 잡아야 한다.

### 3. forwarded header 신뢰 체인을 검증해야 한다

현재 앱은 prod에서 `forward-headers-strategy: native` 와 `app.rate-limit.trust-forwarded-headers: true` 를 사용한다.
`CloudFront -> ALB -> app` 체인으로 바뀌면 실제 클라이언트 IP, scheme, rate limit 기준이 기대대로 유지되는지 확인이 필요하다.

관련 메모:

- `docs/Idea/0027_Tomcat_내부_프록시_설정_검토/idea.md`

## 권장 판단

운영 배포 기준으로는 `nginx` 컨테이너를 계속 쓰는 것보다 `CloudFront + S3 + ALB + private backend` 가 더 적합하다.

다만 적용 방식은 두 가지를 구분해야 한다.

### 권장안

- `CloudFront` 1개 아래에서
  - 정적 경로는 `S3`
  - `/api/*` 는 `ALB`

이렇게 path 기반으로 나눈다.

이 방식이면 same-origin 성격을 유지하기 쉬워서 현재 앱의 세션/쿠키/CORS 가정과 가장 잘 맞는다.

### 비권장안

- 프론트는 `CloudFront`
- API는 별도 public 도메인으로 `ALB`

이 방식은 인프라는 단순해 보여도, 브라우저 단에서는 cross-origin이 되므로 CORS/쿠키 정책을 추가로 설계해야 한다.

## 재검토 조건

다음 중 하나가 필요해지면 이 아이디어를 운영 결정으로 승격한다.

- AWS 운영 배포 구조를 확정한다.
- 정적 자산 배포를 backend 배포와 분리해야 한다.
- 글로벌 CDN 캐시 정책과 무중단 프론트 배포가 중요해진다.
- `nginx` 컨테이너를 운영에서 제거하고 싶다.
