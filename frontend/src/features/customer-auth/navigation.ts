const SAFE_RETURN_TO_BASE = "https://happygallery.local";

export function resolveSafeReturnTo(value: string | null | undefined): string {
  if (!value?.startsWith("/") || value.startsWith("//")) {
    return "/";
  }

  try {
    const url = new URL(value, SAFE_RETURN_TO_BASE);
    if (url.origin !== SAFE_RETURN_TO_BASE) {
      return "/";
    }
    return `${url.pathname}${url.search}${url.hash}`;
  } catch {
    return "/";
  }
}

interface BuildAuthPageHrefOptions {
  redirectTo?: string;
  claim?: boolean;
}

export function buildAuthPageHref(
  path: "/login" | "/signup" | "/forgot-password",
  options: BuildAuthPageHrefOptions = {},
) {
  const searchParams = new URLSearchParams();

  const redirectTo = resolveSafeReturnTo(options.redirectTo);
  if (redirectTo !== "/") {
    searchParams.set("redirect", redirectTo);
  }

  if (options.claim) {
    searchParams.set("claim", "1");
  }

  const query = searchParams.toString();
  return query ? `${path}?${query}` : path;
}
