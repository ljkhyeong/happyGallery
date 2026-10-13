import { expect, test } from "@playwright/test";
import {
  clearSsrUpstreamFixtures,
  homeSsrFixtures,
  replaceSsrUpstreamFixtures,
} from "./ssr-upstream-fixture";

// 운영 배포 후 점검(deploy/k3s/scripts/verify.sh)과 같은 조건이다. 화면을 바꾸다 조건을 깨면
// 롤아웃 뒤가 아니라 PR smoke에서 실패한다. 정규식 문자열은 validate.sh가 verify.sh와 같은지 확인한다.
const ROOT_H1_PATTERN = new RegExp("<h1[^>]*>[^<]*해피갤러리[^<]*</h1>");
const SITE_ORIGIN = "https://happy-gallery.com";

test.afterEach(async () => {
  await clearSsrUpstreamFixtures();
});

test("@smoke 홈 SSR 문서는 운영 배포 후 점검의 H1·canonical·CSP nonce 조건을 만족한다", async ({ request }) => {
  await replaceSsrUpstreamFixtures(...homeSsrFixtures({ workshop: { name: "해피갤러리" } }));

  const response = await request.get("/");
  expect(response.status()).toBe(200);
  expect(response.headers()["content-type"]).toMatch(/^text\/html/i);
  const html = await response.text();

  expect(html).toContain(`<link rel="canonical" href="${SITE_ORIGIN}/"`);
  expect(html).toMatch(ROOT_H1_PATTERN);
  const csp = response.headers()["content-security-policy-report-only"] ?? "";
  const nonce = /'nonce-([^']+)'/.exec(csp)?.[1];
  expect(nonce, "CSP Report-Only 헤더에 요청별 nonce가 없습니다.").toBeTruthy();
  expect(html).toContain(`nonce="${nonce}"`);
});
