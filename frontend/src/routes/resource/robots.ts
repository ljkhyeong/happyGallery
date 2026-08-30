import { SITE_ORIGIN } from "@/shared/seo/metadata";

export function loader() {
  const body = [
    "User-agent: *",
    "Allow: /",
    `Sitemap: ${SITE_ORIGIN}/sitemap.xml`,
    "",
  ].join("\n");

  return new Response(body, {
    headers: {
      "Content-Type": "text/plain; charset=utf-8",
      "Cache-Control": "public, max-age=3600",
    },
  });
}
