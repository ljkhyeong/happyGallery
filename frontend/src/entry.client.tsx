import { startTransition, StrictMode } from "react";
import { hydrateRoot } from "react-dom/client";
import { HydratedRouter } from "react-router/dom";
import { initSentry } from "@/shared/lib/sentry";
import { CspNonceContext, readBrowserCspNonce } from "@/shared/seo/CspJsonLd";

initSentry();
const cspNonce = readBrowserCspNonce();

startTransition(() => {
  hydrateRoot(
    document,
    <StrictMode>
      <CspNonceContext value={cspNonce}>
        <HydratedRouter />
      </CspNonceContext>
    </StrictMode>,
  );
});
