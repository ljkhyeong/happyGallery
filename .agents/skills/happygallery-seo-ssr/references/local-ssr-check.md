# 로컬 SSR 검색 응답 확인

운영 빌드를 Node SSR 서버로 띄워 검색엔진이 받는 첫 응답을 확인한다. 임의 경로 404·`/terms/unknown`·robots는 백엔드 없이 확인할 수 있다. 공개 상세·목록·sitemap은 공개 API가 필요하며 백엔드 준비는 `happygallery-ui-verification`을 따른다.

```bash
cd frontend
npm run build
PORT=3100 HOST=127.0.0.1 INTERNAL_API_ORIGIN=http://127.0.0.1:8080 npm start
```

다른 터미널에서 확인한다. `{id}`는 활성 상품 ID, `{inactiveId}`는 비활성·미존재 ID로 바꾼다. 클래스·이벤트·공지 상세도 같은 방식으로 본다.

```bash
base=http://127.0.0.1:3100

# 상태 코드: 아래 경로는 모두 404, 활성 상세는 200
for p in /__missing__ /products/007 /terms/unknown /products/{inactiveId} /products/{id}; do
  printf '%s ' "$p"; curl -s -o /dev/null -w '%{http_code}\n' "$base$p"
done

# robots: text/plain, 전체 허용, 대표 Sitemap 줄
curl -sSi "$base/robots.txt"

# sitemap: application/xml, https://happy-gallery.com URL만, lastmod 없음, client-only 경로 없음
curl -sSi "$base/sitemap.xml" | head -30

# 첫 HTML: title·robots·canonical·og:url·H1. 봇 UA 응답도 같은지 본다.
for ua in Mozilla/5.0 Googlebot; do
  curl -sS -A "$ua" "$base/products/{id}" \
    | grep -oE '<title>[^<]*</title>|<meta name="robots"[^>]*>|<link rel="canonical"[^>]*>|<meta property="og:url"[^>]*>|<h1[^>]*>[^<]*</h1>'
done

# JSON-LD: 모든 블록이 JSON으로 파싱되고 기대 @type이 있는지 본다.
curl -sS "$base/products/{id}" | node -e '
  let html = "";
  process.stdin.on("data", (chunk) => html += chunk).on("end", () => {
    for (const [, body] of html.matchAll(/<script type="application\/ld\+json"[^>]*>([\s\S]*?)<\/script>/g)) {
      console.log([JSON.parse(body)].flat().map((node) => node["@type"]).join(", "));
    }
  });'

# client-only 화면: noindex,nofollow
curl -sS "$base/my" | grep -oE '<meta name="robots"[^>]*>'
```

- 실패 응답(404·5xx)에서도 robots meta가 `noindex`인지 본다.
- 공개 API를 멈춘 상태의 공개 상세는 200이 아닌 5xx와 noindex여야 한다.
