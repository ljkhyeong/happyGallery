import { defineConfig } from "@playwright/test";

const frontendPort = Number(process.env.PLAYWRIGHT_FRONTEND_PORT ?? 3000);
const baseURL = process.env.PLAYWRIGHT_BASE_URL ?? `http://127.0.0.1:${frontendPort}`;
const mfaFrontendPort = Number(
  process.env.PLAYWRIGHT_MFA_FRONTEND_PORT ?? frontendPort + 2,
);
const mfaBaseURL = process.env.PLAYWRIGHT_MFA_BASE_URL
  ?? `http://127.0.0.1:${mfaFrontendPort}`;
const skipMfaWebServer = process.env.PLAYWRIGHT_SKIP_MFA_WEB_SERVER === "1";

const sharedWebServer = {
  command: `npm run dev -- --host 127.0.0.1 --port ${frontendPort}`,
  env: {
    ...process.env,
    VITE_TOSS_CLIENT_KEY: process.env.VITE_TOSS_CLIENT_KEY ?? "test_ck_e2e",
  },
  url: baseURL,
  reuseExistingServer: !process.env.CI,
  stdout: "ignore" as const,
  stderr: "pipe" as const,
  timeout: 120_000,
};

const mfaWebServer = {
  command: `npm run dev -- --host 127.0.0.1 --port ${mfaFrontendPort}`,
  env: {
    ...process.env,
    VITE_TOSS_CLIENT_KEY: process.env.VITE_TOSS_CLIENT_KEY ?? "test_ck_e2e",
    VITE_REQUIRE_ADMIN_MFA_ENROLLMENT: "true",
  },
  url: mfaBaseURL,
  reuseExistingServer: false,
  stdout: "ignore" as const,
  stderr: "pipe" as const,
  timeout: 120_000,
};

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
  projects: [
    {
      name: "app",
      testIgnore: /admin-auth-mfa\.spec\.ts/,
    },
    {
      name: "admin-mfa",
      testMatch: /admin-auth-mfa\.spec\.ts/,
      use: { baseURL: mfaBaseURL },
    },
  ],
  use: {
    baseURL,
    trace: "on-first-retry",
    screenshot: "only-on-failure",
    video: "retain-on-failure",
  },
  webServer: process.env.PLAYWRIGHT_SKIP_WEB_SERVER === "1"
    ? undefined
    : skipMfaWebServer ? [sharedWebServer] : [sharedWebServer, mfaWebServer],
});
