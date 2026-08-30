import type { Page } from "@playwright/test";

export async function skipExternalFonts({ page }: { page: Page }) {
  await page.route(/^https:\/\/(?:fonts\.googleapis\.com\/|fonts\.gstatic\.com\/|cdn\.jsdelivr\.net\/gh\/orioncactus\/pretendard)/, (route) => route.abort());
}
