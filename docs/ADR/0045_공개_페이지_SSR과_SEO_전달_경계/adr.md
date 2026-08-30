# ADR-0045: 공개 페이지 SSR과 SEO 전달 경계

**날짜**: 2026-08-22
**상태**: Accepted

## 배경

기존 프런트엔드는 모든 URL에 빈 React root와 공통 메타데이터만 있는
`index.html`을 반환했다. 상품·클래스·이벤트·공지 본문과 경로별 메타데이터는
브라우저에서 JavaScript가 실행된 뒤에만 생성됐다. 존재하지 않는 경로도 SPA
fallback으로 HTTP 200을 받았고, sitemap·canonical·`og:url`이 없었다.

운영 대표 origin은 `https://happy-gallery.com`으로 확정했다. 상품과 클래스,
이벤트와 공지는 관리자가 운영 중 변경하므로 배포 시점 prerender만으로는
검색 문서의 최신성과 상세 404를 함께 보장할 수 없다.

## 결정

### 1. 공개 유입 경로는 React Router Framework Mode SSR로 제공한다

- 홈, 상품, 클래스, 단체 수업, 이벤트, 공지, 약관·정책·사업자 정보를
  요청 시점 SSR로 렌더링한다.
- 회원, 결제, 관리자 경로는 검색 노출 대상이 아니므로 client-only 경계 아래서
  기존 CSR 흐름을 유지한다.
- 공개 loader는 프런트엔드 Node 런타임에서 `INTERNAL_API_ORIGIN`의 백엔드 공개
  API를 호출한다. 백엔드 404는 문서 404로, 일시적 장애는 검색 색인을
  허용하지 않는 장애 응답으로 반환한다.
- 요청 간 React Query cache가 섞이지 않도록 SSR 요청마다 `QueryClient`를 생성한다.
  브라우저 저장소는 client hydration 이후에만 읽는다.

### 2. 검색 메타데이터와 구조화 데이터는 route loader 데이터에서 생성한다

- canonical과 Open Graph URL은 요청 `Host`가 아니라 고정된
  `https://happy-gallery.com`을 기준으로 생성한다.
- 상세 title, description, image는 실제 상품·클래스·이벤트·공지 응답을 사용한다.
- 홈의 `LocalBusiness`와 `Organization`은 관리자가 관리하는 `WorkshopProfile`을
  단일 원본으로 삼는다. 자유 문자열 영업시간을 요일·시간 구조로 임의 파싱하지 않는다.
- 상품은 `Product`/`Offer`, 클래스는 `Course`, 공지는 `Article`로 표현한다.
  현재 도메인의 이벤트는 프로모션도 포함하므로 schema.org `Event`로 단정하지 않는다.
- 현재 버전의 `/terms`, `/privacy`만 sitemap에 포함한다. 과거 버전은
  `noindex,follow`, 알 수 없는 버전은 404로 처리한다.

### 3. robots, sitemap과 HTTP 상태를 SSR 런타임이 소유한다

- `/robots.txt`는 공개 콘텐츠 수집을 허용하고
  `Sitemap: https://happy-gallery.com/sitemap.xml`을 제공한다.
- `/sitemap.xml`은 활성 상품·클래스, 현재 공개 이벤트, 공개 공지와 고정
  공개 경로만 포함한다. 신뢰할 수 있는 수정 시각이 없는 현재 모델에서
  `lastmod`를 임의로 만들지 않는다.
- 알 수 없는 경로와 존재하지 않거나 비활성인 공개 상세는 실제 HTTP 404를 반환한다.
- `/healthz`는 frontend Node 프로세스의 startup·readiness·liveness probe로 사용한다.

### 4. 운영 frontend는 정적 Nginx 대신 Node SSR 프로세스로 배포한다

- Traefik Ingress의 `/api/* -> app`, 나머지 `-> frontend`라우팅은 유지한다.
- app ingress는 frontend Pod가 보내는 SSR 내부 요청을 8080에서만 허용하며 NetworkPolicy에 해당 흐름을 명시한다.
- CSP는 응답별 nonce를 생성하는 SSR 서버가 `Content-Security-Policy-Report-Only`로
  한 번만 설정한다. Ingress에 중복 설정하지 않는다.
- 운영 서버는 React Router의 공식 Express adapter를 사용하되 request access log는
  남기지 않는다. 결제 callback과 회원·관리자 query를 Pod 로그로 보내지 않는 기준은
  ADR-0028을 따른다.
- 현재 대표 호스트는 `happy-gallery.com`이며 HTTP는 HTTPS로 영구 전환한다.
  별칭 호스트는 DNS·인증서를 실제로 운영하기 전에 추가하지 않는다.

## 결과

### 장점

- JavaScript 실행 전에도 공개 본문, 경로별 메타데이터와 구조화 데이터를 제공한다.
- 관리자 변경을 재배포 없이 검색 문서에 반영하고 soft 404를 없앤다.
- 회원·결제·관리자 상태를 SSR에 옮기지 않아 개인별 cache·세션 위험을 줄인다.

### 비용

- frontend가 정적 파일 서버가 아닌 상시 Node 프로세스가 되어 CPU·메모리·헬스 감시가 필요하다.
- 공개 문서 요청이 백엔드 공개 API 가용성에 의존한다.
- 공개·client-only 라우트와 서버·브라우저 코드 경계를 계속 검증해야 한다.

## 검증 기준

- `curl https://happy-gallery.com/products/{activeId}`의 HTML에 상품명 H1, title,
  canonical, Open Graph, JSON-LD가 포함된다.
- 비활성·미존재 상세와 임의 경로는 HTTP 404를 반환한다.
- `/robots.txt`는 `text/plain`, `/sitemap.xml`은 XML content type으로 응답하며 sitemap에
  HTTPS 대표 URL과 공개 대상만 있다.
- 회원·결제·관리자 경로는 `noindex`를 유지한다.
- 프론트 이미지는 Node 런타임으로 `/healthz`에 응답하고, app ingress는 frontend
  Pod에서 오는 SSR 내부 호출을 8080에서만 허용한다.
