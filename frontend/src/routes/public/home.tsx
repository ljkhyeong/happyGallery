import type { Route } from "./+types/home";
import heroWorkshop from "@/assets/happygallery/hero-workshop.jpg";
import { HomePage } from "@/pages/HomePage";
import {
  buildBreadcrumbJsonLd,
  buildSeoMeta,
  buildWebSiteJsonLd,
  buildWebPageJsonLd,
  DEFAULT_SEO_DESCRIPTION,
  DEFAULT_SEO_TITLE,
} from "@/shared/seo/metadata";
import { buildLocalBusinessJsonLd } from "@/shared/seo/schemas";
import { CspJsonLd } from "@/shared/seo/CspJsonLd";
import {
  loadClasses,
  loadEvents,
  loadNotices,
  loadProducts,
  loadWorkshop,
} from "@/shared/seo/serverApi.server";

export async function loader({ request }: Route.LoaderArgs) {
  const [products, classes, events, notices, workshop] = await Promise.all([
    loadProducts(request.signal),
    loadClasses(request.signal),
    loadEvents(request.signal),
    loadNotices(request.signal),
    loadWorkshop(request.signal),
  ]);
  return { products, classes, events, notices, workshop };
}

export function meta({ loaderData }: Route.MetaArgs) {
  return buildSeoMeta({
    title: DEFAULT_SEO_TITLE,
    description: DEFAULT_SEO_DESCRIPTION,
    pathname: "/",
    image: heroWorkshop,
    indexable: Boolean(loaderData),
  });
}

export default function HomeRoute({ loaderData }: Route.ComponentProps) {
  return (
    <>
      <CspJsonLd value={[
        buildWebSiteJsonLd(DEFAULT_SEO_DESCRIPTION),
        buildWebPageJsonLd("/", DEFAULT_SEO_TITLE, DEFAULT_SEO_DESCRIPTION),
        buildBreadcrumbJsonLd([]),
        buildLocalBusinessJsonLd(loaderData.workshop, heroWorkshop),
      ]} />
      <HomePage
        initialProducts={loaderData.products}
        initialClasses={loaderData.classes}
        initialEvents={loaderData.events}
        initialNotices={loaderData.notices}
        initialWorkshop={loaderData.workshop}
      />
    </>
  );
}

export { PublicRouteErrorBoundary as ErrorBoundary } from "@/shared/seo/PublicRouteErrorBoundary";
