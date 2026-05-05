import { normalizePhone } from "@/shared/validation/phone";

interface BuildAuthPageHrefOptions {
  redirectTo?: string;
  claim?: boolean;
  phone?: string;
  name?: string;
}

export function buildAuthPageHref(
  path: "/login" | "/signup",
  options: BuildAuthPageHrefOptions = {},
) {
  const searchParams = new URLSearchParams();

  if (options.redirectTo && options.redirectTo !== "/") {
    searchParams.set("redirect", options.redirectTo);
  }

  if (options.claim) {
    searchParams.set("claim", "1");
  }

  if (path === "/signup") {
    if (options.phone) {
      searchParams.set("phone", normalizePhone(options.phone));
    }
    if (options.name) {
      searchParams.set("name", options.name);
    }
  }

  const query = searchParams.toString();
  return query ? `${path}?${query}` : path;
}
