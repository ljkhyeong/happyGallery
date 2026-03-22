# Tomcat Internal Proxies 설정 검토

## 현재 상태

`application-prod.yml`에 `server.forward-headers-strategy: native`를 설정해 Tomcat `RemoteIpValve`를 활성화했다.
이 valve는 `X-Forwarded-For`/`X-Forwarded-Proto` 헤더를 신뢰할 프록시 IP를 `internal-proxies` 패턴으로 제한한다.

### 기본 internal-proxies 패턴

Tomcat 기본값은 RFC1918 사설 IP를 포함한다:

```
10\.\d{1,3}\.\d{1,3}\.\d{1,3}
192\.168\.\d{1,3}\.\d{1,3}
172\.1[6-9]\.\d{1,3}\.\d{1,3}|172\.2[0-9]\.\d{1,3}\.\d{1,3}|172\.3[0-1]\.\d{1,3}\.\d{1,3}
127\.\d{1,3}\.\d{1,3}\.\d{1,3}
169\.254\.\d{1,3}\.\d{1,3}
::1
0:0:0:0:0:0:0:1
```

### 현재 구성에서 문제 없는 이유

Docker Compose 내부 네트워크는 `172.x.x.x` 대역을 사용하므로, Nginx → App 간 통신은 기본 패턴에 포함된다.

## 재검토가 필요한 시점

Nginx 앞에 **외부 프록시(AWS ALB, CloudFront, Cloudflare 등)** 가 추가되면:

1. 외부 프록시의 IP가 `internal-proxies` 패턴에 포함되지 않아 `X-Forwarded-For` 헤더가 무시됨
2. `request.getRemoteAddr()`가 외부 프록시 IP를 반환해 rate limiting이 프록시 단위로 동작
3. `X-Forwarded-Proto`가 무시되어 `Secure` 쿠키 설정이 오동작할 수 있음

## 해결 방향

### 옵션 A: internal-proxies 확장

```yaml
server:
  tomcat:
    remoteip:
      internal-proxies: "10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|172\\.1[6-9]\\.\\d{1,3}\\.\\d{1,3}|172\\.2[0-9]\\.\\d{1,3}\\.\\d{1,3}|172\\.3[0-1]\\.\\d{1,3}\\.\\d{1,3}|192\\.168\\.\\d{1,3}\\.\\d{1,3}|<ALB-IP-RANGE>"
```

ALB 등의 IP 대역을 패턴에 추가한다. AWS ALB는 VPC 내부 IP를 사용하므로 VPC CIDR을 추가하면 된다.

### 옵션 B: trusted-proxies (Spring Boot 4.x)

Spring Boot 4.x에서 `server.tomcat.remoteip.trusted-proxies` 속성이 추가되었는지 확인 필요.
`forward-headers-strategy: framework`로 전환하면 Spring의 `ForwardedHeaderFilter`가 처리하며, 이 필터는 모든 `X-Forwarded-*` 헤더를 신뢰한다 (보안상 프록시 뒤에서만 사용).

### 옵션 C: 모든 프록시 신뢰 (주의 필요)

```yaml
server:
  tomcat:
    remoteip:
      internal-proxies: ".*"
```

외부에서 `X-Forwarded-For`를 조작해 rate limiting을 우회할 수 있으므로, 반드시 앞단 프록시가 헤더를 덮어쓰는 구성에서만 사용한다.

## 판단 기준

- Docker Compose 단독 배포: 현재 기본값 유지
- ALB/NLB 추가 시: 옵션 A (VPC CIDR 추가) 적용
- CloudFront/Cloudflare 추가 시: 옵션 B 또는 C 검토 + 앞단에서 `X-Forwarded-For` 덮어쓰기 확인
