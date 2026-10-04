---
name: happygallery-seo-ssr
description: happyGallery의 공개 페이지 SSR loader·title·meta·canonical·Open Graph·JSON-LD·robots·sitemap·IndexNow 대상·HTTP 404·공개 이미지 성능을 변경할 때 사용한다. 일반 화면·폼·React Query는 happygallery-frontend-flows, Ingress·TLS·운영 주소 검사는 happygallery-deploy-ops 스킬을 사용한다.
---

# happyGallery 공개 SSR·검색 노출

## 규칙

- 기준은 ADR-0045와 PRD-0001 공개 화면 절이다. 경로는 `frontend/src/routes.ts`, 메타·JSON-LD·sitemap·서버 조회는 `frontend/src/shared/seo/`에서 시작한다.
- 상품 데이터는 `happygallery-product-flows`, 이벤트는 `happygallery-benefit-flows`, 후기 공개는 `happygallery-review-flows`, 측정 지표 수집은 `happygallery-observability-flows`, 공개 API 응답은 `api-contract`를 함께 사용한다.
- 검색 대상은 `routes/public`에 두고 `loader`·`meta`·`PublicRouteErrorBoundary`를 함께 export한다. 그 외 화면은 `client-only-layout.tsx` 아래에 둔다. 하위 route가 `meta`를 export하면 레이아웃의 `noindex,nofollow`가 대체되므로 robots를 직접 넣는다.
- 메타는 `buildSeoMeta`로 만든다. canonical·`og:url`·JSON-LD URL은 요청 Host가 아닌 `SITE_ORIGIN` 기준의 같은 경로를 쓰고, loader가 실패하면 `indexable: Boolean(loaderData)`로 noindex가 되게 한다.
- 상세 ID는 `requirePublicId`로 검사한다. 잘못된 ID·백엔드 404는 문서 404, 모르는 경로는 `not-found.tsx`의 404, 백엔드 장애는 noindex 오류 응답이다. 200 빈 화면이나 빈 목록으로 바꾸지 않는다.
- 상품 검색 조건은 `readProductFilters`로 정제하되 canonical·sitemap은 `/products`만 쓴다. 정책 문서는 현재 버전만 색인·sitemap에 넣고 과거 버전은 `noindex,follow`, 모르는 버전은 404다.
- JSON-LD는 `CspJsonLd`로 SSR HTML에 nonce와 함께 넣고 `metadata.ts`·`schemas.ts` builder를 재사용한다. 이벤트를 `Event`로 단정하지 않고, 영업시간 문자열을 구조화하지 않으며, `lastmod`처럼 응답에 없는 값을 만들지 않는다.
- 공개 loader는 `serverApi.server.ts`로 cookie·인증 헤더 없이 공개 API만 호출하고 데이터를 모두 기다린 뒤 렌더링한다. 개인 데이터와 브라우저 저장소는 hydration 뒤 읽고, QueryClient는 SSR 요청마다 만들며, loader 응답은 `useLoaderBackedQuery`로 같은 조건의 query key에만 넣는다.
- SSR HTML에는 공용 캐시 헤더를 추가하지 않는다. 관리자 변경과 404 전환을 재배포 없이 반영해야 하며 공용 캐시는 robots(1시간)·sitemap(5분)만 쓴다. 봇 UA는 `entry.server.tsx`의 `onAllReady`로 완성된 HTML을 받는다.
- 공개 경로나 목록 API를 바꾸면 `routes/resource/sitemap.ts`와 `deploy/k3s/scripts/notify-indexnow.rb`의 `SOURCES`·`PAGE_PATH`를 함께 고친다. 목록 API가 페이지 단위로 바뀌면 두 곳 모두 전체 페이지를 수집하게 바꾼다.
- robots는 전체 허용과 대표 sitemap 선언만 둔다. 비공개 화면은 Disallow가 아닌 meta `noindex,nofollow`로 막는다.
- 공개 이미지 `/api/v1/media/images`는 현재 공개 참조만 `no-store`로 준다(ADR-0037). og·JSON-LD 이미지는 `absoluteSiteUrl`로 절대 URL을 만들고 없으면 번들 기본 이미지를 쓴다. 상세 대표 이미지는 aspect-ratio 컨테이너로 자리를 잡고, 첫 화면 밖 이미지에만 `loading="lazy"`를 쓴다.

## 검증

- meta·JSON-LD helper: `frontend`에서 `node --test tests/unit/seo.test.mjs`, 정책 버전은 `tests/unit/policyVersions.test.mjs`, IndexNow 키 경로는 `tests/unit/indexnow.test.mjs`를 실행한다. 전체 `test:unit`으로 넓히지 않는다.
- 라우트·loader·`entry.server.tsx`·`root.tsx`: `npm run build` 후 [로컬 SSR 확인](references/local-ssr-check.md)으로 상태 코드, 첫 HTML의 head·H1·JSON-LD, robots·sitemap을 curl로 확인한다.
- loader 데이터와 query cache 연결: `PLAYWRIGHT_SKIP_MFA_WEB_SERVER=1 npx playwright test tests/e2e/ssr-loader-cache.spec.ts`.
- IndexNow 수집 대상: 저장소 루트에서 `ruby deploy/k3s/scripts/tests/indexnow-test.rb`. 운영 주소의 SSR·404·robots·sitemap 검사는 deploy-ops 스킬의 `verify.sh`가 담당한다.
