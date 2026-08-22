import type { Route } from "./+types/sitemap";
import {
  loadClasses,
  loadEvents,
  loadNotices,
  loadProducts,
} from "@/shared/seo/serverApi.server";
import { buildSitemapXml } from "@/shared/seo/sitemap";

const STATIC_PATHS = [
  "/",
  "/classes",
  "/group-classes",
  "/products",
  "/events",
  "/terms",
  "/privacy",
  "/business-info",
] as const;

export async function loader({ request }: Route.LoaderArgs) {
  const [products, classes, events, notices] = await Promise.all([
    loadProducts(request.signal),
    loadClasses(request.signal),
    loadEvents(request.signal),
    loadNotices(request.signal),
  ]);
  const body = buildSitemapXml([
    ...STATIC_PATHS,
    ...products.map((product) => `/products/${product.id}`),
    ...classes.map((bookingClass) => `/classes/${bookingClass.id}`),
    ...events.map((event) => `/events/${event.id}`),
    ...notices.map((notice) => `/notices/${notice.id}`),
  ]);

  return new Response(body, {
    headers: {
      "Content-Type": "application/xml; charset=utf-8",
      "Cache-Control": "public, max-age=300",
    },
  });
}
