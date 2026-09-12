import { expect, test } from "@playwright/test";
import type { ClassResponse } from "../../src/generated/api/booking";
import {
  clearSsrUpstreamFixtures,
  replaceSsrUpstreamFixtures,
  ssrApiFixture,
} from "./ssr-upstream-fixture";

const craftClass: ClassResponse = {
  id: 41,
  name: "가죽 공예",
  category: "LEATHER",
  durationMin: 90,
  price: 30000,
  bufferMin: 30,
  capacity: 6,
  passEligible: true,
  description: "가죽 소품을 만드는 수업입니다.",
  imageUrl: null,
  preparationInfo: null,
  targetAudience: null,
  status: "ACTIVE",
};
const classes: ClassResponse[] = [
  craftClass,
  { ...craftClass, id: 42, name: "위빙 공예", category: "WEAVING" },
  { ...craftClass, id: 43, name: "특별 공예", passEligible: false },
  { ...craftClass, id: 44, name: "향수 만들기", category: "PERFUME" },
];

async function registerClasses(rows: ClassResponse[]) {
  await replaceSsrUpstreamFixtures(
    ssrApiFixture("/classes", rows),
    ssrApiFixture("/workshop", { name: "해피갤러리" }),
    { ...ssrApiFixture("/me", { code: "UNAUTHORIZED", message: "로그인이 필요합니다." }), status: 401 },
    ssrApiFixture("/payments/pass-policy", { totalPrice: 120000, totalCredits: 4, validityDays: 90 }),
  );
}

test.afterEach(async ({ page }) => {
  await page.close();
  await clearSsrUpstreamFixtures();
});

test("이용권 구매 전에 사용 가능한 수업을 찾고 필터를 새로고침·뒤로 가기에도 유지한다", async ({ page }, testInfo) => {
  await registerClasses(classes);
  const pageErrors: string[] = [];
  page.on("pageerror", (error) => pageErrors.push(error.message));
  await page.setViewportSize({ width: 1280, height: 900 });
  await page.goto("/passes/purchase");
  await page.getByRole("link", { name: "이용권 사용 가능 수업 보기" }).click();

  const filter = page.getByRole("checkbox", { name: "이용권 사용 가능 수업만" });
  const cards = page.locator(".class-catalog-item");
  await expect(page).toHaveURL(/\/classes\?passEligible=true$/);
  await expect(filter).toBeChecked();
  await expect(cards).toHaveCount(2);
  await expect(cards.getByRole("heading")).toHaveText(["가죽 공예", "위빙 공예"]);
  await expect(page.getByRole("link", { name: "가죽 공예 예약하기" }))
    .toHaveAttribute("href", "/bookings/new?classId=41");
  await page.screenshot({ path: testInfo.outputPath("desktop.png"), fullPage: true });

  await page.setViewportSize({ width: 390, height: 844 });
  await page.screenshot({ path: testInfo.outputPath("mobile.png"), fullPage: true });
  await filter.click();
  await expect(filter).not.toBeChecked();
  await expect(cards).toHaveCount(4);
  await expect(page).toHaveURL(/\/classes$/);
  await page.goBack();
  await expect(filter).toBeChecked();
  await expect(cards).toHaveCount(2);
  await page.reload();
  await expect(filter).toBeChecked();
  await expect(cards).toHaveCount(2);
  expect(pageErrors).toEqual([]);
});

test("이용권 대상 수업이 없으면 서버 첫 화면도 빈 결과를 표시하고 전체 수업으로 돌아간다", async ({ page, browser, baseURL }) => {
  await registerClasses(classes.slice(2));
  const path = "/classes?passEligible=true&source=pass";
  const ssrContext = await browser.newContext({ javaScriptEnabled: false });
  try {
    const ssrPage = await ssrContext.newPage();
    await ssrPage.goto(new URL(path, baseURL).href);
    await expect(ssrPage.locator(".class-catalog-item")).toHaveCount(0);
    await expect(ssrPage.getByText("이용권을 사용할 수 있는 수업이 없습니다.")).toBeVisible();
  } finally {
    await ssrContext.close();
  }

  await page.goto(path);
  await page.getByRole("button", { name: "전체 수업 보기" }).click();
  await expect(page).toHaveURL(/\/classes\?source=pass$/);
  await expect(page.getByRole("checkbox", { name: "이용권 사용 가능 수업만" })).not.toBeChecked();
  await expect(page.locator(".class-catalog-item")).toHaveCount(2);
  await page.goBack();
  await expect(page.getByText("이용권을 사용할 수 있는 수업이 없습니다.")).toBeVisible();
});
