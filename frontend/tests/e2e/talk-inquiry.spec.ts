import { expect, test, type Page } from "@playwright/test";
import type { ProductDetailResponse } from "../../src/generated/api/product";
import { clearSsrUpstreamFixtures, replaceSsrUpstreamFixtures, ssrApiFixture } from "./ssr-upstream-fixture";
import { skipExternalFonts } from "./external-fonts";

declare global {
  interface Window {
    talkCopyMode: "success" | "denied" | "pending";
    talkCopyTexts: string[];
    finishTalkCopy?: () => void;
  }
}

test.beforeEach(skipExternalFonts);
test.afterEach(clearSsrUpstreamFixtures);

async function prepare(page: Page, clipboardAvailable = true) {
  await page.addInitScript((available) => {
    window.talkCopyMode = "denied";
    window.talkCopyTexts = [];
    Object.defineProperty(navigator, "clipboard", { configurable: true, value: available ? {
      writeText: async (text: string) => {
        window.talkCopyTexts.push(text);
        if (window.talkCopyMode === "denied") throw new DOMException("권한 없음", "NotAllowedError");
        if (window.talkCopyMode === "pending") await new Promise<void>((resolve) => { window.finishTalkCopy = resolve; });
      },
    } : undefined });
  }, clipboardAvailable);
  const product: ProductDetailResponse = {
    id: 80, name: "맞춤 가죽 키링", type: "MADE_TO_ORDER", price: 10000, available: true,
    category: null, description: null, imageUrl: null, specification: "가죽 키링",
    careInstructions: null, productionLeadDays: 7, stockQuantity: 5, optionGroups: [], variants: [],
  };
  await replaceSsrUpstreamFixtures(ssrApiFixture("/products/80", product));
  await page.route("**/api/v1/**", (route) => {
    const path = new URL(route.request().url()).pathname;
    const json = (body: unknown, status = 200) => route.fulfill({
      status, contentType: "application/json", body: JSON.stringify(body),
    });
    if (path === "/api/v1/me") return json({ code: "UNAUTHORIZED" }, 401);
    if (path === "/api/v1/workshop") return json({ name: "해피갤러리", naverTalkUrl: "https://talk.naver.com/example" });
    if (path === "/api/v1/policies/current") return json({
      terms: { version: "2026-09", documentPath: "/terms" },
      privacy: { version: "2026-09", documentPath: "/privacy" },
    });
    if (path === "/api/v1/classes") return json([{
      id: 27, name: "가죽 공예", category: "LEATHER", durationMin: 60, price: 30000,
      bufferMin: 30, capacity: 6, passEligible: true, status: "ACTIVE",
    }]);
    if (path === "/api/v1/products/80") return route.fallback();
    if (path === "/api/v1/orders/policy") return json({
      shippingFee: 3000, madeToOrderConsentVersion: "2026-09", madeToOrderConsentText: "주문제작 조건에 동의합니다.",
    });
    if (path === "/api/v1/products/80/qna/page") return json({ content: [], hasMore: false, nextCursor: null });
    if (path === "/api/v1/products/80/reviews") return json({
      content: [], filteredCount: 0, hasMore: false, nextCursor: null,
      summary: { averageRating: 0, reviewCount: 0, histogram: { rating1: 0, rating2: 0, rating3: 0, rating4: 0, rating5: 0 } },
    });
    return json([]);
  });
}

test("수업 문의 복사 실패는 직접 복사와 재시도를 제공하고 희망일 변경을 반영한다", async ({ page }, testInfo) => {
  await prepare(page);
  await page.setViewportSize({ width: 1280, height: 900 });
  await page.goto("/bookings/new?classId=27");
  const copy = page.getByRole("button", { name: "문의 문구 복사", exact: true });
  await expect(copy).toBeVisible();
  await copy.click();
  const fallback = page.getByRole("textbox", { name: "직접 복사할 문의 문구" });
  await expect(fallback).toHaveValue("해피갤러리 클래스 일정 문의드립니다.\n클래스: 가죽 공예\n희망일: 날짜 협의");
  await expect(page.getByRole("alert")).toContainText("복사하지 못했습니다");
  await fallback.focus();
  expect(await fallback.evaluate((element: HTMLTextAreaElement) => element.selectionEnd - element.selectionStart))
    .toBe((await fallback.inputValue()).length);
  await page.screenshot({ path: testInfo.outputPath("desktop-copy-failure.png"), fullPage: true });

  await page.evaluate(() => { window.talkCopyMode = "success"; });
  await copy.click();
  await expect(page.getByRole("status")).toHaveText("문의 문구를 복사했습니다.");
  await expect(fallback).toHaveCount(0);
  expect(page.context().pages()).toHaveLength(1);

  await page.getByLabel("문의할 희망일").fill("2099-01-15");
  await expect(page.getByText("문의 문구를 복사했습니다.", { exact: true })).toHaveCount(0);
  await copy.click();
  await expect(page.getByRole("status")).toHaveText("문의 문구를 복사했습니다.");
  expect(await page.evaluate(() => window.talkCopyTexts.at(-1))).toContain("희망일: 2099. 01. 15.");
});

test("상품 문의는 복사 기능이 없어도 문구를 보여주고 톡톡을 별도 창으로 연다", async ({ page }, testInfo) => {
  await prepare(page, false);
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/products/80");
  await expect(page.getByRole("button", { name: "문의 문구 복사", exact: true })).toBeVisible();
  await page.getByRole("button", { name: "문의 문구 복사", exact: true }).click();
  await expect(page.getByRole("textbox", { name: "직접 복사할 문의 문구" }))
    .toHaveValue("해피갤러리 상품 맞춤 변경 문의드립니다.\n상품: 맞춤 가죽 키링\n원하는 변경: ");
  await expect(page.getByRole("alert").filter({ hasText: "복사하지 못했습니다" })).toBeVisible();
  const talk = page.getByRole("link", { name: "맞춤 변경 네이버톡톡 상담" });
  await expect(talk).toHaveAttribute("href", "https://talk.naver.com/example");
  await expect(talk).toHaveAttribute("rel", "noopener noreferrer");
  await page.screenshot({ path: testInfo.outputPath("mobile-copy-failure.png"), fullPage: true });
  await page.context().route("https://talk.naver.com/**", (route) => route.fulfill({
    contentType: "text/html", body: "<title>네이버톡톡 대역</title>",
  }));
  const popupPromise = page.waitForEvent("popup");
  await talk.click();
  const popup = await popupPromise;
  await expect(popup).toHaveURL("https://talk.naver.com/example");
  await popup.close();
});

test("희망일을 바꾼 뒤 끝난 이전 복사는 새 문구의 복사 성공으로 표시하지 않는다", async ({ page }) => {
  await prepare(page);
  await page.goto("/bookings/new?classId=27");
  await expect(page.getByRole("button", { name: "문의 문구 복사", exact: true })).toBeVisible();
  await page.evaluate(() => { window.talkCopyMode = "pending"; });
  await page.getByRole("button", { name: "문의 문구 복사", exact: true }).click();
  await expect(page.getByRole("button", { name: "복사 중...", exact: true })).toBeDisabled();
  await page.getByLabel("문의할 희망일").fill("2099-01-15");
  await page.evaluate(() => window.finishTalkCopy?.());
  await expect(page.getByText("문의 문구를 복사했습니다.", { exact: true })).toHaveCount(0);
  await page.evaluate(() => { window.talkCopyMode = "success"; });
  await page.getByRole("button", { name: "문의 문구 복사", exact: true }).click();
  await expect(page.getByRole("status")).toHaveText("문의 문구를 복사했습니다.");
  expect(await page.evaluate(() => window.talkCopyTexts)).toEqual([
    "해피갤러리 클래스 일정 문의드립니다.\n클래스: 가죽 공예\n희망일: 날짜 협의",
    "해피갤러리 클래스 일정 문의드립니다.\n클래스: 가죽 공예\n희망일: 2099. 01. 15.",
  ]);
});
