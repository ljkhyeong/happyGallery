import { expect, test, type BrowserContext, type Route } from "@playwright/test";

const ADMIN_TOKEN_KEY = "hg_admin_token";
const EMPTY_CART_VERSION = "0".repeat(64);
const ONE_PIXEL_PNG = Buffer.from(
  "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=",
  "base64",
);

async function fulfillJson(route: Route, body: unknown, status = 200) {
  await route.fulfill({
    status,
    contentType: "application/json",
    body: JSON.stringify(body),
  });
}

async function installCsrfCookie(context: BrowserContext, baseURL: string | undefined) {
  if (!baseURL) throw new Error("Playwright baseURL이 필요합니다.");
  await context.addCookies([{
    name: "XSRF-TOKEN",
    value: "review-hardening-csrf",
    url: baseURL,
  }]);
}

test("@smoke @admin 후기 신고의 되돌릴 수 없는 판단은 확인 뒤 한 번만 전송한다", async ({ page }) => {
  const reportStatuses = new Map<number, "PENDING" | "ACCEPTED" | "REJECTED">([
    [91, "PENDING"],
    [92, "PENDING"],
  ]);
  const decisions: Array<Record<string, unknown>> = [];
  const statusChanges: Array<Record<string, unknown>> = [];
  const replyChanges: Array<Record<string, unknown>> = [];
  const evidenceImageRequests: Array<{ path: string; authorization?: string }> = [];
  const hiddenReviewImageRequests: Array<{ path: string; authorization?: string }> = [];
  let hiddenPublicImageRequests = 0;
  let currentReviewRevision = 3;
  let currentReviewVersion = 5;
  let currentReviewContent = "현재 후기 본문";
  let currentReviewStatus: "PUBLISHED" | "HIDDEN" = "PUBLISHED";
  let currentOfficialReply: Record<string, unknown> | null = null;
  let evidenceRetryEnabled = false;
  let adminReviewGetCount = 0;
  let reportDetailGetCount = 0;
  const report = (id: number) => ({
    id,
    reviewId: id === 91 ? 11 : 12,
    reporterUserId: 501,
    reason: id === 91 ? "ABUSIVE" : "SPAM",
    detail: id === 91 ? "모욕적인 표현이 있습니다." : null,
    evidence: {
      id: id + 100,
      contentRevision: 2,
      rating: id === 91 ? 1 : 3,
      content: id === 91 ? "신고 판단 확인 대상 후기" : "신고 반려 확인 대상 후기",
      editedAt: null,
      provenance: "LIVE",
      imagesComplete: true,
      imageUrls: [`/api/v1/admin/review-evidence/${id + 100}/images/0`],
      capturedAt: "2026-08-09T10:00:00",
    },
    snapshotStatus: "PUBLISHED",
    status: reportStatuses.get(id),
    decisionNote: reportStatuses.get(id) === "ACCEPTED" ? "운영 정책 위반 확인" : null,
    decidedByAdminId: reportStatuses.get(id) === "PENDING" ? null : 7,
    decidedAt: reportStatuses.get(id) === "PENDING" ? null : "2026-08-09T12:00:00",
    createdAt: "2026-08-09T10:00:00",
  });
  const adminReview = () => ({
    id: 11,
    targetType: "PRODUCT",
    targetId: 42,
    targetName: "신고 대상 작품",
    sourceType: "ORDER_ITEM",
    sourceId: 701,
    userId: 601,
    authorName: "후기 작성자",
    rating: 1,
    content: currentReviewContent,
    contentRevision: currentReviewRevision,
    version: currentReviewVersion,
    status: currentReviewStatus,
    hiddenReason: currentReviewStatus === "HIDDEN" ? "운영 정책 위반" : null,
    hiddenByAdminId: currentReviewStatus === "HIDDEN" ? 7 : null,
    hiddenAt: currentReviewStatus === "HIDDEN" ? "2026-08-09T12:30:00" : null,
    createdAt: "2026-08-09T09:00:00",
    updatedAt: "2026-08-09T12:00:00",
    edited: currentReviewRevision > 3,
    editedAt: currentReviewRevision > 3 ? "2026-08-09T12:00:00" : null,
    verifiedTransaction: true,
    officialReply: currentOfficialReply,
    helpfulCount: 0,
    images: currentReviewStatus === "HIDDEN"
      ? [{
          id: 301,
          imageUrl: "/api/v1/media/images/current-hidden.png",
          sortOrder: 0,
          createdAt: "2026-08-09T09:10:00",
        }]
      : [],
  });

  await page.addInitScript(([key, token]) => {
    sessionStorage.setItem(key, token);
  }, [ADMIN_TOKEN_KEY, "review-hardening-admin-token"] as const);
  await page.addInitScript(() => {
    const created: string[] = [];
    const revoked: string[] = [];
    const nativeCreateObjectUrl = URL.createObjectURL.bind(URL);
    const nativeRevokeObjectUrl = URL.revokeObjectURL.bind(URL);
    URL.createObjectURL = (blob: Blob) => {
      const objectUrl = nativeCreateObjectUrl(blob);
      created.push(objectUrl);
      return objectUrl;
    };
    URL.revokeObjectURL = (objectUrl: string) => {
      revoked.push(objectUrl);
      nativeRevokeObjectUrl(objectUrl);
    };
    Object.defineProperty(window, "__reviewEvidenceBlobUrls", {
      value: { created, revoked },
    });
  });

  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const { pathname } = new URL(request.url());

    if (pathname === "/api/v1/me") {
      await fulfillJson(route, { code: "UNAUTHORIZED", message: "로그인이 필요합니다." }, 401);
      return;
    }
    if (pathname === "/api/v1/workshop") {
      await fulfillJson(route, { name: "해피갤러리" });
      return;
    }
    const evidenceImageMatch = pathname.match(
      /^\/api\/v1\/admin\/review-evidence\/(\d+)\/images\/(\d+)$/,
    );
    if (evidenceImageMatch) {
      evidenceImageRequests.push({
        path: pathname,
        authorization: request.headers().authorization,
      });
      if (evidenceImageMatch[1] === "192" && !evidenceRetryEnabled) {
        await fulfillJson(route, {
          code: "INTERNAL_ERROR",
          message: "사진 증거를 불러오지 못했습니다.",
          requestId: "review-evidence-error",
        }, 500);
        return;
      }
      await route.fulfill({ status: 200, contentType: "image/png", body: ONE_PIXEL_PNG });
      return;
    }
    if (pathname === "/api/v1/admin/reviews/11/images/301") {
      hiddenReviewImageRequests.push({
        path: pathname,
        authorization: request.headers().authorization,
      });
      await route.fulfill({ status: 200, contentType: "image/png", body: ONE_PIXEL_PNG });
      return;
    }
    if (pathname === "/api/v1/media/images/current-hidden.png") {
      hiddenPublicImageRequests += 1;
      await fulfillJson(route, { code: "NOT_FOUND", message: "이미지를 찾을 수 없습니다." }, 404);
      return;
    }
    if (pathname === "/api/v1/admin/reviews") {
      await fulfillJson(route, { content: [], nextCursor: null, hasMore: false });
      return;
    }
    if (pathname === "/api/v1/admin/reviews/11" && request.method() === "GET") {
      adminReviewGetCount += 1;
      await fulfillJson(route, adminReview());
      return;
    }
    if (pathname === "/api/v1/admin/reviews/11/status" && request.method() === "PATCH") {
      const body = request.postDataJSON() as {
        status: "PUBLISHED" | "HIDDEN";
        reason: string | null;
        expectedContentRevision: number;
        expectedVersion: number;
      };
      statusChanges.push(body);
      if (statusChanges.length === 1) {
        currentReviewRevision = 4;
        currentReviewVersion = 6;
        currentReviewContent = "작성자가 수정한 최신 후기";
        await fulfillJson(route, {
          code: "REVIEW_CONTENT_CHANGED",
          message: "후기 본문 revision이 변경되었습니다.",
        }, 409);
        return;
      }
      currentReviewStatus = body.status;
      currentReviewVersion += 1;
      await fulfillJson(route, adminReview());
      return;
    }
    if (pathname === "/api/v1/admin/reviews/11/reply" && request.method() === "PUT") {
      const body = request.postDataJSON() as { content: string; expectedVersion: number };
      replyChanges.push({ method: "PUT", ...body });
      if (replyChanges.length === 1) {
        currentReviewVersion += 1;
        currentOfficialReply = {
          content: "다른 관리자의 최신 답글",
          adminUserId: 8,
          createdAt: "2026-08-09T12:40:00",
          edited: false,
          editedAt: null,
        };
        await fulfillJson(route, {
          code: "CONFLICT",
          message: "후기 운영 version이 변경되었습니다.",
        }, 409);
        return;
      }
      currentReviewVersion += 1;
      currentOfficialReply = {
        content: body.content,
        adminUserId: 7,
        createdAt: "2026-08-09T12:45:00",
        edited: true,
        editedAt: "2026-08-09T12:45:00",
      };
      await fulfillJson(route, adminReview());
      return;
    }
    if (pathname === "/api/v1/admin/reviews/11/reply" && request.method() === "DELETE") {
      const expectedVersion = Number(new URL(request.url()).searchParams.get("expectedVersion"));
      replyChanges.push({ method: "DELETE", expectedVersion });
      currentReviewVersion += 1;
      currentOfficialReply = null;
      await fulfillJson(route, adminReview());
      return;
    }
    if (pathname === "/api/v1/admin/reviews/11/moderation-actions") {
      await fulfillJson(route, []);
      return;
    }
    if (pathname === "/api/v1/admin/review-reports" && request.method() === "GET") {
      const requestedStatus = new URL(request.url()).searchParams.get("status");
      const content = [report(91), report(92)]
        .filter((item) => !requestedStatus || item.status === requestedStatus)
        .map(({ id, reviewId, reason, snapshotStatus, status, createdAt }) => ({
          id,
          reviewId,
          reason,
          snapshotStatus,
          status,
          createdAt,
        }));
      await fulfillJson(route, { content, nextCursor: null, hasMore: false });
      return;
    }
    const decisionMatch = pathname.match(/^\/api\/v1\/admin\/review-reports\/(\d+)$/);
    if (decisionMatch && request.method() === "GET") {
      reportDetailGetCount += 1;
      await fulfillJson(route, report(Number(decisionMatch[1])));
      return;
    }
    if (decisionMatch && request.method() === "PATCH") {
      const reportId = Number(decisionMatch[1]);
      const body = request.postDataJSON() as {
        decision: "ACCEPTED" | "REJECTED";
        note: string | null;
      };
      decisions.push({ reportId, ...body });
      reportStatuses.set(reportId, body.decision);
      await fulfillJson(route, report(reportId));
      return;
    }

    await fulfillJson(route, []);
  });

  await page.goto("/admin?view=reviews");
  await page.getByRole("tab", { name: "신고 관리" }).click();

  const reportCard = page.locator(".admin-review-report-card").filter({
    hasText: "후기 #11",
  });
  const imageFailureCard = page.locator(".admin-review-report-card").filter({
    hasText: "후기 #12",
  });
  expect(reportDetailGetCount).toBe(0);
  await reportCard.getByRole("button", { name: "신고 상세 검토" }).click();
  await imageFailureCard.getByRole("button", { name: "신고 상세 검토" }).click();
  await expect(reportCard.getByText("본문 버전 2", { exact: true })).toBeVisible();
  await expect(reportCard.getByAltText("당시 후기 사진 1")).toHaveAttribute("src", /^blob:/);
  await expect(imageFailureCard.getByRole("alert")).toContainText("사진 증거를 불러오지 못했습니다.");
  expect(evidenceImageRequests).toContainEqual({
    path: "/api/v1/admin/review-evidence/191/images/0",
    authorization: "Bearer review-hardening-admin-token",
  });
  expect(evidenceImageRequests).toContainEqual({
    path: "/api/v1/admin/review-evidence/192/images/0",
    authorization: "Bearer review-hardening-admin-token",
  });
  await expect(page.locator('img[src*="/api/v1/admin/review-evidence/"]')).toHaveCount(0);

  const failedImageRequestsBeforeRetry = evidenceImageRequests
    .filter(({ path }) => path.includes("/192/")).length;
  evidenceRetryEnabled = true;
  await imageFailureCard.getByRole("button", { name: "다시 시도" }).click();
  await expect(imageFailureCard.getByAltText("당시 후기 사진 1")).toHaveAttribute("src", /^blob:/);
  await expect.poll(() => evidenceImageRequests.filter(({ path }) => (
    path.includes("/192/")
  )).length).toBeGreaterThan(failedImageRequestsBeforeRetry);

  const createdBlobUrls = await page.evaluate(() => (
    (window as unknown as Window & {
      __reviewEvidenceBlobUrls: { created: string[]; revoked: string[] };
    }).__reviewEvidenceBlobUrls.created.slice()
  ));
  expect(createdBlobUrls.length).toBeGreaterThanOrEqual(2);
  await reportCard.getByRole("button", { name: "현재 후기 확인·관리" }).click();
  await expect(page).toHaveURL(/view=reviews.*reviewId=11/);
  await expect(page.getByRole("heading", { name: "신고 대상 후기 #11" })).toBeFocused();
  await expect.poll(() => page.evaluate(() => {
    const blobUrls = (window as unknown as Window & {
      __reviewEvidenceBlobUrls: { created: string[]; revoked: string[] };
    }).__reviewEvidenceBlobUrls;
    return blobUrls.created.length > 0
      && blobUrls.created.every((objectUrl) => blobUrls.revoked.includes(objectUrl));
  })).toBe(true);

  let focusedReviewCard = page.locator(".admin-review-card").filter({ hasText: "현재 후기 본문" });
  await focusedReviewCard.getByRole("button", { name: "숨기기" }).click();
  await focusedReviewCard.getByLabel("비공개 사유").fill("부적절한 내용");
  await focusedReviewCard.getByRole("button", { name: "비공개 확정" }).click();

  await expect.poll(() => statusChanges[0]).toEqual({
    status: "HIDDEN",
    reason: "부적절한 내용",
    expectedContentRevision: 3,
    expectedVersion: 5,
  });
  focusedReviewCard = page.locator(".admin-review-card").filter({ hasText: "작성자가 수정한 최신 후기" });
  await expect.poll(() => adminReviewGetCount).toBeGreaterThanOrEqual(2);
  await expect(focusedReviewCard.getByText("본문 버전 4", { exact: true })).toBeVisible();
  await expect(page.getByText("최신 상태를 다시 불러왔습니다.", { exact: false }).last())
    .toBeVisible();
  await focusedReviewCard.getByRole("button", { name: "비공개 확정" }).click();
  await expect.poll(() => statusChanges[1]).toEqual({
    status: "HIDDEN",
    reason: "부적절한 내용",
    expectedContentRevision: 4,
    expectedVersion: 6,
  });
  await expect(focusedReviewCard.getByText("비공개", { exact: true })).toBeVisible();
  await expect(focusedReviewCard.getByAltText("비공개 후기 첨부 사진 1"))
    .toHaveAttribute("src", /^blob:/);
  expect(hiddenReviewImageRequests).toContainEqual({
    path: "/api/v1/admin/reviews/11/images/301",
    authorization: "Bearer review-hardening-admin-token",
  });
  expect(hiddenPublicImageRequests).toBe(0);

  const createReplyButton = focusedReviewCard.getByRole("button", { name: "공식 답글 작성" });
  await createReplyButton.click();
  const replyTextbox = focusedReviewCard.getByRole("textbox", { name: "공식 답글", exact: true });
  await expect(replyTextbox).toBeFocused();
  await focusedReviewCard.getByRole("button", { name: "취소", exact: true }).click();
  await expect(createReplyButton).toBeFocused();

  await createReplyButton.click();
  await replyTextbox.fill("공방에서 확인한 답글");
  await focusedReviewCard.getByRole("button", { name: "답글 저장" }).click();
  await expect.poll(() => replyChanges[0]).toEqual({
    method: "PUT",
    content: "공방에서 확인한 답글",
    expectedVersion: 7,
  });
  await expect(focusedReviewCard.getByText("다른 관리자의 최신 답글", { exact: true })).toBeVisible();
  await expect(page.getByText("최신 상태를 다시 불러왔습니다.", { exact: false }).last())
    .toBeVisible();

  await focusedReviewCard.getByRole("button", { name: "답글 수정" }).click();
  await expect(replyTextbox).toBeFocused();
  await replyTextbox.fill("공방에서 확인한 답글");
  await focusedReviewCard.getByRole("button", { name: "답글 저장" }).click();
  await expect.poll(() => replyChanges[1]).toEqual({
    method: "PUT",
    content: "공방에서 확인한 답글",
    expectedVersion: 8,
  });
  await expect(focusedReviewCard.getByText("공방에서 확인한 답글", { exact: true })).toBeVisible();
  await expect(focusedReviewCard.getByRole("button", { name: "답글 수정" })).toBeFocused();
  await focusedReviewCard.getByRole("button", { name: "답글 삭제" }).click();
  await focusedReviewCard.locator(".admin-review-reply-delete")
    .getByRole("button", { name: "삭제", exact: true })
    .click();
  await expect.poll(() => replyChanges[2]).toEqual({ method: "DELETE", expectedVersion: 9 });
  await expect(focusedReviewCard.getByText("공방에서 확인한 답글", { exact: true })).toHaveCount(0);

  await page.getByRole("tab", { name: "신고 관리" }).click();
  await expect(page).not.toHaveURL(/reviewId=/);
  await expect.poll(() => page.evaluate(() => {
    const blobUrls = (window as unknown as Window & {
      __reviewEvidenceBlobUrls: { created: string[]; revoked: string[] };
    }).__reviewEvidenceBlobUrls;
    return blobUrls.created.every((objectUrl) => blobUrls.revoked.includes(objectUrl));
  })).toBe(true);
  const refreshedReportCard = page.locator(".admin-review-report-card").filter({
    hasText: "후기 #11",
  });
  await refreshedReportCard.getByRole("button", { name: "신고 상세 검토" }).click();
  await refreshedReportCard.getByLabel(/처리 메모/).fill("운영 정책 위반 확인");
  await refreshedReportCard.getByRole("button", { name: "위반 인정", exact: true }).click();

  const confirmation = page.getByRole("dialog", { name: "위반 신고로 인정할까요?" });
  await expect(confirmation).toBeVisible();
  await expect(confirmation.getByText("저장하면 이 신고를 다시 판단할 수 없습니다.", { exact: false }))
    .toBeVisible();
  expect(decisions).toEqual([]);

  await confirmation.getByRole("button", { name: "취소" }).click();
  await expect(confirmation).toHaveCount(0);
  expect(decisions).toEqual([]);

  await refreshedReportCard.getByRole("button", { name: "위반 인정", exact: true }).click();
  await page.getByRole("dialog", { name: "위반 신고로 인정할까요?" })
    .getByRole("button", { name: "위반 인정 확정" })
    .click();

  await expect.poll(() => decisions).toEqual([{
    reportId: 91,
    decision: "ACCEPTED",
    note: "운영 정책 위반 확인",
  }]);
  await expect(refreshedReportCard).toHaveCount(0);

  const rejectedReportCard = page.locator(".admin-review-report-card").filter({
    hasText: "후기 #12",
  });
  await rejectedReportCard.getByRole("button", { name: "신고 상세 검토" }).click();
  await rejectedReportCard.getByRole("button", { name: "신고 반려", exact: true }).click();
  const rejectConfirmation = page.getByRole("dialog", { name: "신고를 반려할까요?" });
  await expect(rejectConfirmation.getByText("저장하면 이 신고를 다시 판단할 수 없습니다.", { exact: false }))
    .toBeVisible();
  await rejectConfirmation.getByRole("button", { name: "신고 반려 확정" }).click();

  await expect.poll(() => decisions).toEqual([
    { reportId: 91, decision: "ACCEPTED", note: "운영 정책 위반 확인" },
    { reportId: 92, decision: "REJECTED", note: null },
  ]);
  await expect(rejectedReportCard).toHaveCount(0);
  await expect(page.getByRole("heading", { name: "후기 신고" })).toBeFocused();
});

test("@smoke 공개 후기 반응은 불러온 페이지별로 조회하고 도움돼요 변경을 반영한다", async ({
  baseURL,
  context,
  page,
}) => {
  await installCsrfCookie(context, baseURL);
  const pageErrors: string[] = [];
  const consoleErrors: string[] = [];
  const failedResponses: string[] = [];
  page.on("pageerror", (error) => pageErrors.push(error.stack ?? error.message));
  page.on("console", (message) => {
    if (message.type() === "error") consoleErrors.push(message.text());
  });
  page.on("response", (response) => {
    if (response.status() >= 400) {
      failedResponses.push(`${response.status()} ${new URL(response.url()).pathname}`);
    }
  });
  const reactionRequests: number[][] = [];
  let helpfulMutationCount = 0;
  let firstReviewHelpful = false;

  const publicReview = (id: number, content: string) => ({
    id,
    rating: 5,
    content,
    authorName: "리뷰**",
    sourceType: "ORDER_ITEM",
    verifiedTransaction: true,
    createdAt: "2026-08-09T10:00:00",
    updatedAt: "2026-08-09T10:00:00",
    edited: false,
    editedAt: null,
    officialReply: null,
    helpfulCount: id === 11 && firstReviewHelpful ? 1 : 0,
    images: id === 11 ? [{
      id: 111,
      imageUrl: "/api/v1/media/images/broken-review-image.png",
      sortOrder: 0,
      createdAt: "2026-08-09T10:00:00",
    }] : [],
  });
  const summary = {
    reviewCount: 3,
    averageRating: 5,
    histogram: { rating1: 0, rating2: 0, rating3: 0, rating4: 0, rating5: 3 },
  };

  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const { pathname } = url;

    if (pathname === "/api/v1/me") {
      await fulfillJson(route, {
        id: 501,
        email: "review-hardening@example.com",
        name: "후기 검증 회원",
        phone: "01012345678",
        phoneVerified: true,
        localPasswordEnabled: true,
      });
      return;
    }
    if (pathname === "/api/v1/me/cart") {
      await fulfillJson(route, { cartVersion: EMPTY_CART_VERSION, items: [], totalAmount: 0 });
      return;
    }
    if (pathname === "/api/v1/me/notifications/unread-count") {
      await fulfillJson(route, { count: 0 });
      return;
    }
    if (pathname === "/api/v1/workshop") {
      await fulfillJson(route, { name: "해피갤러리" });
      return;
    }
    if (pathname === "/api/v1/products/42") {
      await fulfillJson(route, {
        id: 42,
        name: "페이지 반응 검증 작품",
        description: null,
        category: "테스트",
        type: "READY_STOCK",
        price: 12000,
        imageUrl: null,
        available: true,
        specification: null,
        careInstructions: null,
        productionLeadDays: null,
      });
      return;
    }
    if (pathname === "/api/v1/orders/policy") {
      await fulfillJson(route, {
        shippingFee: 3000,
        madeToOrderConsentVersion: "2026-08",
        madeToOrderConsentText: "주문제작 동의",
      });
      return;
    }
    if (pathname === "/api/v1/media/images/broken-review-image.png") {
      await route.fulfill({ status: 200, contentType: "image/png", body: "invalid-image" });
      return;
    }
    if (pathname === "/api/v1/products/42/reviews") {
      const secondPage = url.searchParams.get("cursor") === "review-next-page";
      await fulfillJson(route, {
        summary,
        filteredCount: 3,
        content: secondPage
          ? [publicReview(21, "두 번째 페이지 후기")]
          : [
              publicReview(11, "첫 번째 페이지 후기 A"),
              publicReview(12, "첫 번째 페이지 후기 B"),
            ],
        nextCursor: secondPage ? null : "review-next-page",
        hasMore: !secondPage,
      });
      return;
    }
    if (pathname === "/api/v1/me/reviews/reactions") {
      const reviewIds = url.searchParams.getAll("reviewIds").map(Number);
      reactionRequests.push(reviewIds);
      await fulfillJson(route, reviewIds.map((reviewId) => ({
        reviewId,
        helpfulByMe: reviewId === 11 && firstReviewHelpful,
        reportedByMe: false,
        ownedByMe: reviewId === 12,
        canInteract: reviewId !== 12,
      })));
      return;
    }
    if (pathname === "/api/v1/me/reviews/11/helpful" && request.method() === "PUT") {
      helpfulMutationCount += 1;
      firstReviewHelpful = true;
      await fulfillJson(route, { helpfulByMe: true, helpfulCount: 1 });
      return;
    }
    if (pathname === "/api/v1/products/42/qna/page"
      || pathname === "/api/v1/me/products/42/qna/page") {
      await fulfillJson(route, { content: [], nextCursor: null, hasMore: false });
      return;
    }

    await fulfillJson(route, []);
  });

  await page.goto("/products/42");
  const firstCard = page.locator(".public-review-card").filter({ hasText: "첫 번째 페이지 후기 A" });
  await expect(firstCard.getByRole("button", { name: "도움돼요 0" })).toBeEnabled();
  await expect(firstCard.getByRole("status", { name: "후기 사진 1 불러오기 실패" }))
    .toContainText("사진을 표시할 수 없습니다.");
  await expect(firstCard.getByRole("link", { name: "후기 사진 1 크게 보기" })).toHaveCount(0);
  const ownedCard = page.locator(".public-review-card").filter({ hasText: "첫 번째 페이지 후기 B" });
  await expect(ownedCard.getByText("내 후기", { exact: true })).toBeVisible();
  await expect(ownedCard.getByRole("button", { name: "도움돼요 0" })).toBeDisabled();
  await expect(ownedCard.getByRole("button", { name: "신고", exact: true })).toBeDisabled();

  const requestsBeforeNextPage = reactionRequests.length;
  await page.getByRole("button", { name: "후기 더 보기" }).click();
  await expect(page.getByText("두 번째 페이지 후기", { exact: true })).toBeVisible();
  await expect.poll(() => reactionRequests.slice(requestsBeforeNextPage)).toContainEqual([21]);
  expect(reactionRequests.some((reviewIds) => (
    reviewIds.includes(11) && reviewIds.includes(21)
  ))).toBe(false);

  await firstCard.getByRole("button", { name: "도움돼요 0" }).click();
  await expect.poll(() => helpfulMutationCount).toBe(1);
  await expect(firstCard.getByRole("button", { name: "도움돼요 1" }))
    .toHaveAttribute("aria-pressed", "true");
  expect({ pageErrors, consoleErrors, failedResponses }).toEqual({
    pageErrors: [],
    consoleErrors: [],
    failedResponses: [],
  });
});

test("@smoke 작성 가능한 후기 이용 내역은 커서 다음 페이지까지 이어서 표시한다", async ({ page }) => {
  const opportunityCursors: Array<string | null> = [];
  const opportunity = (sourceId: number, targetName: string) => ({
    sourceId,
    sourceType: "ORDER_ITEM",
    orderId: sourceId + 100,
    bookingId: null,
    targetType: "PRODUCT",
    targetId: sourceId + 200,
    targetName,
    completedAt: "2026-08-09T10:00:00",
  });

  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const { pathname } = url;

    if (pathname === "/api/v1/me") {
      await fulfillJson(route, {
        id: 701,
        email: "review-opportunity@example.com",
        name: "후기 기회 회원",
        phone: "01012345678",
        phoneVerified: true,
        localPasswordEnabled: true,
      });
      return;
    }
    if (pathname === "/api/v1/me/cart") {
      await fulfillJson(route, { cartVersion: EMPTY_CART_VERSION, items: [], totalAmount: 0 });
      return;
    }
    if (pathname === "/api/v1/me/notifications/unread-count") {
      await fulfillJson(route, { count: 0 });
      return;
    }
    if (pathname === "/api/v1/workshop") {
      await fulfillJson(route, { name: "해피갤러리" });
      return;
    }
    if (pathname === "/api/v1/me/reviews/opportunities") {
      const cursor = url.searchParams.get("cursor");
      opportunityCursors.push(cursor);
      const secondPage = cursor === "opportunity-next-page";
      await fulfillJson(route, {
        content: secondPage
          ? [opportunity(3, "세 번째 후기 기회")]
          : [
              opportunity(1, "첫 번째 후기 기회"),
              opportunity(2, "두 번째 후기 기회"),
            ],
        nextCursor: secondPage ? null : "opportunity-next-page",
        hasMore: !secondPage,
      });
      return;
    }
    if (pathname === "/api/v1/me/reviews") {
      await fulfillJson(route, { content: [], nextCursor: null, hasMore: false });
      return;
    }

    await fulfillJson(route, []);
  });

  await page.goto("/my/reviews");
  await expect(page.getByText("첫 번째 후기 기회", { exact: true })).toBeVisible();
  await expect(page.getByText("두 번째 후기 기회", { exact: true })).toBeVisible();
  await expect(page.getByText("불러온 2건", { exact: true })).toBeVisible();

  await page.getByRole("button", { name: "작성 가능한 이용 내역 더 보기" }).click();
  await expect(page.getByText("세 번째 후기 기회", { exact: true })).toBeVisible();
  await expect(page.getByText("불러온 3건", { exact: true })).toBeVisible();
  expect(opportunityCursors).toContain(null);
  expect(opportunityCursors).toContain("opportunity-next-page");
});

test("@smoke 회원 후기 수정은 최신 content revision을 확인한 뒤 다시 저장한다", async ({
  baseURL,
  context,
  page,
}) => {
  await installCsrfCookie(context, baseURL);
  await page.setViewportSize({ width: 390, height: 844 });
  const updates: Array<{ rating: number; content: string; expectedContentRevision: number }> = [];
  let reviewFetchCount = 0;
  let protectedImageRequests = 0;
  let publicHiddenImageRequests = 0;
  let deleteImageRequests = 0;
  let currentRevision = 8;
  let currentContent = "내가 작성한 원래 후기";
  const memberReview = () => ({
    id: 51,
    targetType: "PRODUCT",
    targetId: 251,
    targetName: "수정 충돌 검증 작품",
    sourceType: "ORDER_ITEM",
    sourceId: 151,
    rating: 5,
    content: currentContent,
    contentRevision: currentRevision,
    status: "HIDDEN",
    hiddenReason: "운영 정책 확인 중",
    createdAt: "2026-08-09T10:00:00",
    updatedAt: "2026-08-09T10:00:00",
    edited: currentRevision > 8,
    editedAt: currentRevision > 8 ? "2026-08-09T11:00:00" : null,
    verifiedTransaction: true,
    officialReply: null,
    helpfulCount: 0,
    images: [{
      id: 61,
      imageUrl: "/api/v1/media/images/member-hidden.png",
      sortOrder: 0,
      createdAt: "2026-08-09T10:10:00",
    }],
  });

  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const { pathname } = new URL(request.url());

    if (pathname === "/api/v1/me") {
      await fulfillJson(route, {
        id: 701,
        email: "review-revision@example.com",
        name: "후기 수정 회원",
        phone: "01012345678",
        phoneVerified: true,
        localPasswordEnabled: true,
      });
      return;
    }
    if (pathname === "/api/v1/me/cart") {
      await fulfillJson(route, { cartVersion: EMPTY_CART_VERSION, items: [], totalAmount: 0 });
      return;
    }
    if (pathname === "/api/v1/me/notifications/unread-count") {
      await fulfillJson(route, { count: 0 });
      return;
    }
    if (pathname === "/api/v1/workshop") {
      await fulfillJson(route, { name: "해피갤러리" });
      return;
    }
    if (pathname === "/api/v1/me/reviews/opportunities") {
      await fulfillJson(route, { content: [], nextCursor: null, hasMore: false });
      return;
    }
    if (pathname === "/api/v1/me/reviews/51/images/61" && request.method() === "GET") {
      protectedImageRequests += 1;
      await route.fulfill({ status: 200, contentType: "image/png", body: ONE_PIXEL_PNG });
      return;
    }
    if (pathname === "/api/v1/me/reviews/51/images/61" && request.method() === "DELETE") {
      deleteImageRequests += 1;
      await route.fulfill({ status: 204, body: "" });
      return;
    }
    if (pathname === "/api/v1/media/images/member-hidden.png") {
      publicHiddenImageRequests += 1;
      await fulfillJson(route, { code: "NOT_FOUND", message: "이미지를 찾을 수 없습니다." }, 404);
      return;
    }
    if (pathname === "/api/v1/me/reviews/51" && request.method() === "PATCH") {
      const body = request.postDataJSON() as {
        rating: number;
        content: string;
        expectedContentRevision: number;
      };
      updates.push(body);
      if (updates.length === 1) {
        currentRevision = 9;
        currentContent = "다른 탭에서 저장한 최신 후기";
        await fulfillJson(route, {
          code: "REVIEW_CONTENT_CHANGED",
          message: "불러온 뒤 후기 내용이 변경되었습니다.",
        }, 409);
        return;
      }
      currentRevision = 10;
      currentContent = body.content;
      await fulfillJson(route, memberReview());
      return;
    }
    if (pathname === "/api/v1/me/reviews" && request.method() === "GET") {
      reviewFetchCount += 1;
      await fulfillJson(route, {
        content: [memberReview()],
        nextCursor: null,
        hasMore: false,
      });
      return;
    }

    await fulfillJson(route, []);
  });

  await page.goto("/my/reviews");
  const card = page.locator(".member-review-card").filter({ hasText: "수정 충돌 검증 작품" });
  await expect(card.getByText("내가 작성한 원래 후기", { exact: true })).toBeVisible();
  await expect(card.getByAltText("등록한 후기 사진 1")).toHaveAttribute("src", /^blob:/);
  expect(protectedImageRequests).toBeGreaterThanOrEqual(1);
  expect(publicHiddenImageRequests).toBe(0);
  const imageDeleteButton = card.getByRole("button", { name: "등록한 후기 사진 1 삭제" });
  const deleteButtonBox = await imageDeleteButton.boundingBox();
  expect(deleteButtonBox?.width).toBeGreaterThanOrEqual(44);
  expect(deleteButtonBox?.height).toBeGreaterThanOrEqual(44);
  await imageDeleteButton.click();
  const imageDeleteDialog = page.getByRole("dialog", { name: "후기 사진 삭제" });
  await expect(imageDeleteDialog).toContainText("삭제한 사진은 복구할 수 없습니다.");
  expect(deleteImageRequests).toBe(0);
  await imageDeleteDialog.getByRole("button", { name: "취소" }).click();
  await expect(imageDeleteButton).toBeFocused();

  const editButton = card.getByRole("button", { name: "수정", exact: true });
  await editButton.click();
  await expect(card.getByRole("radio", { name: "5점" })).toBeFocused();
  await card.getByRole("button", { name: "취소", exact: true }).click();
  await expect(editButton).toBeFocused();

  await editButton.click();
  await card.getByLabel("후기 내용").fill("첫 번째 수정 시도");
  await card.getByRole("button", { name: "수정 저장" }).click();

  await expect.poll(() => updates[0]).toEqual({
    rating: 5,
    content: "첫 번째 수정 시도",
    expectedContentRevision: 8,
  });
  await expect.poll(() => reviewFetchCount).toBeGreaterThanOrEqual(2);
  await expect(card.getByText("다른 화면에서 후기가 변경되었습니다.", {
    exact: false,
  })).toBeVisible();
  await expect(card.getByLabel("후기 내용")).toHaveValue("다른 탭에서 저장한 최신 후기");

  await card.getByLabel("후기 내용").fill("최신 내용을 확인하고 저장한 후기");
  await card.getByRole("button", { name: "수정 저장" }).click();
  await expect.poll(() => updates[1]).toEqual({
    rating: 5,
    content: "최신 내용을 확인하고 저장한 후기",
    expectedContentRevision: 9,
  });
  await expect(card.getByText("최신 내용을 확인하고 저장한 후기", { exact: true })).toBeVisible();
});

test("@smoke 후기 작성 폼은 첫 입력으로 이동하고 취소 후 작성 버튼으로 돌아간다", async ({
  page,
}) => {
  await page.route("**/api/v1/**", async (route) => {
    const { pathname } = new URL(route.request().url());

    if (pathname === "/api/v1/me") {
      await fulfillJson(route, {
        id: 801,
        email: "review-focus@example.com",
        name: "후기 포커스 회원",
        phone: "01012345678",
        phoneVerified: true,
        localPasswordEnabled: true,
      });
      return;
    }
    if (pathname === "/api/v1/workshop") {
      await fulfillJson(route, { name: "해피갤러리" });
      return;
    }
    if (pathname === "/api/v1/me/cart") {
      await fulfillJson(route, { cartVersion: EMPTY_CART_VERSION, items: [], totalAmount: 0 });
      return;
    }
    if (pathname === "/api/v1/me/notifications/unread-count") {
      await fulfillJson(route, { count: 0 });
      return;
    }
    if (pathname === "/api/v1/me/orders/83") {
      await fulfillJson(route, {
        approvalDeadlineAt: null,
        fulfillment: null,
        items: [{
          careInstructions: null,
          orderItemId: 831,
          productId: 42,
          productName: "후기 포커스 검증 작품",
          productType: "READY_STOCK",
          productionLeadDays: null,
          qty: 1,
          specification: null,
          unitPrice: 12000,
        }],
        orderId: 83,
        orderNumber: "HG-REVIEW-FOCUS-83",
        paidAt: "2026-08-09T10:00:00",
        refund: null,
        shippingFee: 0,
        status: "COMPLETED",
        totalAmount: 12000,
      });
      return;
    }
    if (pathname === "/api/v1/me/reviews/orders/83") {
      await fulfillJson(route, []);
      return;
    }
    if (pathname === "/api/v1/me/reviews/products/831/creation-state") {
      await fulfillJson(route, { status: "AVAILABLE" });
      return;
    }
    if (pathname === "/api/v1/me/orders/83/claims") {
      await fulfillJson(route, []);
      return;
    }

    await fulfillJson(route, []);
  });

  await page.goto("/my/orders/83");
  const reviewCard = page.locator(".review-card").filter({ hasText: "후기 포커스 검증 작품" });
  const writeButton = reviewCard.getByRole("button", { name: "후기 작성" });
  await writeButton.click();
  await expect(reviewCard.getByRole("radio", { name: "5점" })).toBeFocused();
  await reviewCard.getByRole("button", { name: "취소", exact: true }).click();
  await expect(writeButton).toBeFocused();
});
