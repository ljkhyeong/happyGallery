// API를 모의하는 E2E spec의 공용 응답·세션·fixture. 실제 백엔드를 쓰는 준비 helper는 support.ts에 둔다.
import { expect, type BrowserContext, type Page, type Route } from "@playwright/test";
import type { ProductDetailResponse } from "../../src/generated/api/product";
import type { PublicReviewPageResponse } from "../../src/generated/api/review";

/** 관리자 화면이 토큰을 읽는 sessionStorage 키. src/features/admin-product/useAdminKey.ts의 TOKEN_KEY와 같다. */
export const ADMIN_TOKEN_KEY = "hg_admin_token";

/** 값을 확인하지 않는 모의 관리자 API에 쓰는 기본 토큰 */
export const MOCK_ADMIN_TOKEN = "e2e-admin-token";

/** 빈 회원 장바구니의 cartVersion */
export const EMPTY_CART_VERSION = "0".repeat(64);

/** 후기가 없는 공개 후기 목록. 공유 객체이므로 spec에서 수정하지 않는다. */
export const EMPTY_REVIEW_PAGE = {
  content: [],
  filteredCount: 0,
  hasMore: false,
  nextCursor: null,
  summary: {
    averageRating: 0,
    reviewCount: 0,
    histogram: { rating1: 0, rating2: 0, rating3: 0, rating4: 0, rating5: 0 },
  },
} satisfies PublicReviewPageResponse;

/** 옵션 없는 재고 작품 상세. 시나리오에 필요한 값만 overrides로 바꾼다. */
export function productDetailFixture(overrides: Partial<ProductDetailResponse> = {}): ProductDetailResponse {
  return {
    id: 42,
    name: "테스트 작품",
    description: null,
    category: "테스트",
    type: "READY_STOCK",
    price: 12000,
    imageUrl: null,
    available: true,
    stockQuantity: 10,
    specification: null,
    careInstructions: null,
    productionLeadDays: null,
    optionGroups: [],
    variants: [],
    ...overrides,
  } satisfies ProductDetailResponse;
}

/** 전체 문서를 다시 받지 않고 React Router의 클라이언트 이동으로 path를 연다. */
export async function navigateInApp(page: Page, path: string) {
  await page.evaluate((nextPath) => {
    window.history.pushState({}, "", nextPath);
    window.dispatchEvent(new PopStateEvent("popstate"));
  }, path);
}

/** API 모의 응답을 JSON으로 보낸다. Playwright는 json 값이 falsy이면 content-type을 붙이지 않으므로 직접 지정한다. */
export async function fulfillJson(route: Route, body: unknown, status = 200) {
  await route.fulfill({ status, contentType: "application/json", json: body });
}

/** 고객 변경 요청이 /auth/csrf를 거치지 않고 보낼 CSRF 쿠키를 넣는다. 헤더 값을 단정할 때만 value를 넘긴다. */
export async function installCsrfCookie(
  context: BrowserContext,
  url: string | undefined,
  value = "e2e-csrf",
) {
  if (!url) throw new Error("Playwright baseURL이 필요합니다.");
  await context.addCookies([{ name: "XSRF-TOKEN", value, url }]);
}

/** 다음 문서부터 관리자 토큰이 저장된 상태로 연다. 토큰 값을 단정할 때만 token을 넘긴다. */
export async function seedAdminSession(page: Page, token = MOCK_ADMIN_TOKEN) {
  await page.addInitScript(([key, value]) => {
    sessionStorage.setItem(key, value);
  }, [ADMIN_TOKEN_KEY, token] as const);
}

/** 모의 관리자 세션으로 관리자 화면의 view를 열고 머리글이 보일 때까지 기다린다. */
export async function openMockedAdmin(page: Page, view: string, token?: string) {
  await seedAdminSession(page, token);
  await page.goto(`/admin?view=${view}`);
  await expect(page.getByRole("heading", { name: "관리자", exact: true })).toBeVisible();
}

/** Toss SDK 대역이 requestPayment 요청을 쌓는 전역 이름. 직접 내려주는 SDK 스크립트도 이 이름을 쓴다. */
export const TOSS_REQUESTS_GLOBAL = "tossRequests";

/** 문서를 열 때마다 결제창 대신 requestPayment 요청만 기록하는 Toss SDK 대역을 설치한다. */
export async function installTossRequestRecorder(page: Page) {
  await page.addInitScript((globalName) => {
    const recorded = globalThis as unknown as Record<string, unknown[]>;
    const sdk = globalThis as unknown as { TossPayments: unknown };
    recorded[globalName] = [];
    sdk.TossPayments = () => ({
      payment: () => ({
        requestPayment: async (request: unknown) => {
          recorded[globalName].push(request);
        },
      }),
    });
  }, TOSS_REQUESTS_GLOBAL);
}

/** installTossRequestRecorder가 기록한 requestPayment 요청을 읽는다. */
export function readTossRequests(page: Page): Promise<unknown[]> {
  return page.evaluate(
    (globalName) => (globalThis as unknown as Record<string, unknown[]>)[globalName],
    TOSS_REQUESTS_GLOBAL,
  );
}
