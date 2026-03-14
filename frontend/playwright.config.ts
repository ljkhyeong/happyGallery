import { defineConfig } from "@playwright/test";

const frontendPort = Number(process.env.PLAYWRIGHT_FRONTEND_PORT ?? 3000);
const baseURL = process.env.PLAYWRIGHT_BASE_URL ?? `http://127.0.0.1:${frontendPort}`;

export default defineConfig({
  testDir: "./tests/e2e",
  testMatch: /.*\.spec\.ts/,
  timeout: 120_000,
  expect: {
    timeout: 10_000,
  },
  fullyParallel: false,
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [["html", { open: "never" }], ["list"]] : "list",
  use: {
    baseURL,
    trace: "on-first-retry",
    screenshot: "only-on-failure",
    video: "retain-on-failure",
  },
  webServer: process.env.PLAYWRIGHT_SKIP_WEB_SERVER === "1"
    ? undefined
    : {
        command: `npm run dev -- --host 127.0.0.1 --port ${frontendPort}`,
        url: baseURL,
        reuseExistingServer: !process.env.CI,
        stdout: "ignore",
        stderr: "pipe",
        timeout: 120_000,
      },
});
