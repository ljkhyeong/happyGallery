---
name: happygallery-deploy-ops
description: happyGallery의 노트북 서버·k3s·Docker Compose·Ingress·TLS·Secret·영속 데이터·백업·복구·배포 장애를 다룰 때 사용한다.
---

# happyGallery 배포·운영

## 시작점

- 목표 구성은 ADR-0037, 실행 절차는 `deploy/k3s/README.md`, 노트북 준비는 `deploy/laptop/README.md`를 확인한다. 실제 접속·배포 상태는 실행 결과로 판단한다.
- 운영 대상은 단일 노트북 k3s다. Docker Compose는 local 프로필의 개발 환경이다. 과거 AWS Idea 문서는 운영 절차로 사용하지 않는다.

## 운영 규칙

- 외부에는 Ingress HTTP/HTTPS만 노출하고 앱·MySQL·Redis·Actuator·모니터링은 내부에 둔다. 직접 앱 접근을 막고 Ingress가 forwarded header를 정리할 때만 proxy header를 신뢰한다.
- MySQL은 PVC를 사용하고 암호화 백업·복구 키를 노트북 밖에 보관한다. Redis는 재생성 가능한 세션·제한 상태이므로 손실 시 세션과 제한 횟수가 초기화된다.
- Secret 생성은 파일별 허용 key만 받는다. 운영 프로필·보안 필수값은 container env로 고정하고 유효 설정이 이를 위반하면 시작을 거부한다.
- `ENCRYPT_KEY`·`HMAC_KEY`는 저장 데이터와 연결된다. 키만 바꾸지 말고 호환 migration·검증한 백업·이전/새 키 복구본을 준비한다.
- 기존 MySQL PVC의 비밀번호 회전은 앱 중지 → DB 계정과 Secret 동시 변경 → MySQL 재시작·확인 → 앱 시작 순서로 한다. 일부 실패하면 앱을 중지 상태로 둔다.
- Redis 비밀번호는 시작 시 읽는다. 앱 중지 후 공유 Secret을 변경하고 Redis를 재시작한 다음 앱을 시작한다.
- 이미지는 commit SHA·digest로 지정하고 이전 이미지·release manifest를 보존한다. 복구용 기반 이미지는 `runtime-images-from-manifest.rb`로 해당 manifest에서 구한다.
- image rollback은 DB를 되돌리지 않는다. 복구할 때 앱 replica를 0으로 두고 Redis를 비우며, 백업 schema·데이터 키와 호환되는 이미지로 복구한 뒤 앱을 시작한다.
- startup/readiness/liveness probe와 최소 30초 종료 유예를 유지한다.
- 같은 origin에서 SSR과 `/api/*`를 분기한다. Node loader는 `INTERNAL_API_ORIGIN=http://app:8080`을 사용하고 API 오류를 HTML로 바꾸지 않는다. `PUBLIC_HOST`와 대표 주소 `https://happy-gallery.com`을 맞춘다.
- 노트북·디스크·전원·네트워크·k3s 장애로 서비스 전체가 중단될 수 있음을 운영 문서에 명시한다.

## 변경별 검증

- manifest·script: `deploy/k3s/scripts/validate.sh`. Secret 예시는 생성 script의 허용 key와 비교하고 필수 설정 덮어쓰기 거절을 확인한다.
- Compose: `docker compose config`. 이미지: 실제 이미지 build·SHA/digest·노트북 CPU 호환·k3s 전달 방법을 확인한다.
- 실제 배포: node·pod·event·PVC·Service·Ingress·인증서·probe·rollout을 확인하고 SSR HTML·404·healthz·robots·sitemap·API JSON을 각각 검사한다.
- 네트워크·인증 변경: 실제 Ingress 경로의 client IP·HTTPS·cookie·CSRF·rate limit과 내부 서비스 직접 접근 차단을 확인한다.
- 영속 데이터 변경: 최근 외부 백업, 키 복구 경로, 실제 restore와 rollback 절차를 확인한다.
