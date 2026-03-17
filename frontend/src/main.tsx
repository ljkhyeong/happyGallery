import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { initSentry } from "@/shared/lib/sentry";
import { App } from "@/app/App";

initSentry();

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
