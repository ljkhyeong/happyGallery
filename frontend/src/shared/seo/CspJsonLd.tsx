import { createContext, useContext } from "react";

export const CspNonceContext = createContext<string | undefined>(undefined);

interface CspJsonLdProps {
  value: Record<string, unknown> | readonly Record<string, unknown>[];
}

export function readBrowserCspNonce(): string | undefined {
  if (typeof document === "undefined") return undefined;
  return document.querySelector<HTMLScriptElement>("script[nonce]")?.nonce || undefined;
}

function serializeJsonLd(value: CspJsonLdProps["value"]): string {
  return JSON.stringify(value)
    .replaceAll("<", "\\u003c")
    .replaceAll("\u2028", "\\u2028")
    .replaceAll("\u2029", "\\u2029");
}

export function CspJsonLd({ value }: CspJsonLdProps) {
  const nonce = useContext(CspNonceContext) ?? readBrowserCspNonce();

  return (
    <script
      type="application/ld+json"
      nonce={nonce}
      suppressHydrationWarning
      dangerouslySetInnerHTML={{ __html: serializeJsonLd(value) }}
    />
  );
}
