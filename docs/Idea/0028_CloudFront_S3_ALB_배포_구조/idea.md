# CloudFront + S3 + ALB 배포 구조 메모

**상태**: 운영 반영 완료

> 현재 운영 배포는 CloudFront를 공용 진입점으로 두고, 정적 파일은 S3에서, API는 ALB 뒤의 ECS Fargate에서 처리한다. 이 문서는 그 구조를 선택한 배경 메모다.

## 현재 구조

```text
사용자 브라우저
  -> CloudFront
       -> S3 (프론트엔드 정적 파일)
       -> /api/* -> ALB
                     -> ECS Fargate
```

CloudFront에서는 두 개의 원본(origin)을 둔다.

- 기본 원본: 비공개 S3 버킷
- `/api/*` 원본: ALB

사용자는 하나의 CloudFront 주소만 보게 된다.

## 왜 이 구조를 선택했나

### 1. 프론트엔드와 API를 한 주소 아래에서 제공하기 쉽다

- 브라우저에서는 프론트엔드와 API가 같은 도메인 아래에 있다.
- 현재 앱의 세션, 쿠키, CORS 가정과 잘 맞는다.

### 2. 정적 파일 운영이 단순해진다

- 프론트엔드 정적 파일은 S3에 배포하고 CloudFront가 캐시한다.
- 백엔드 컨테이너와 정적 파일 배포를 분리할 수 있다.

### 3. 백엔드 공개 범위를 줄일 수 있다

- 백엔드는 비공개 서브넷에 두고, 외부 공개는 ALB까지만 맡길 수 있다.
- 운영용 `nginx` 컨테이너를 별도로 유지하지 않아도 된다.

## 운영 시 계속 확인할 점

### 1. SPA fallback은 CloudFront 기준으로 처리한다

`nginx try_files` 대신 CloudFront에서 `index.html`로 되돌리는 설정이 필요하다.

### 2. 전달 헤더 체인은 실제 운영에서 확인한다

`CloudFront -> ALB -> app` 체인에서 실제 클라이언트 IP, 스킴, 처리율 제한 기준이 기대대로 유지되는지 확인해야 한다.

### 3. 프론트와 API 주소를 분리하면 설계를 다시 봐야 한다

`cdn.example.com`과 `api.example.com`처럼 주소를 완전히 나누면 CORS와 쿠키 정책을 다시 설계해야 한다.

## 관련 문서

- `docs/Idea/0029_GitHub_Actions_CI_CD_배포_Fargate/idea.md`
- `docs/Idea/0039_AWS_배포_설정_베이스라인/idea.md`
