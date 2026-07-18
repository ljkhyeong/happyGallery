import * as Sentry from "@sentry/react";

const dsn = import.meta.env.VITE_SENTRY_DSN;

export function initSentry() {
  if (!dsn) return;

  Sentry.init({
    dsn,
    environment: import.meta.env.VITE_SENTRY_ENVIRONMENT ?? "local",
    release: import.meta.env.VITE_SENTRY_RELEASE,
    tracesSampleRate: 0.1,
    sendDefaultPii: false,
  });
}
