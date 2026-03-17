import * as Sentry from "@sentry/react";

const dsn = import.meta.env.VITE_SENTRY_DSN as string | undefined;

export function initSentry() {
  if (!dsn) return;

  Sentry.init({
    dsn,
    environment: (import.meta.env.VITE_SENTRY_ENVIRONMENT as string) ?? "local",
    release: (import.meta.env.VITE_SENTRY_RELEASE as string) ?? undefined,
    tracesSampleRate: 0.1,
    sendDefaultPii: false,
  });
}
