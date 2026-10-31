import type { Route } from "./+types/notices";
import heroWorkshop from "@/assets/happygallery/hero-workshop.jpg";
import { NoticesPage } from "@/pages/NoticesPage";
import {
  buildBreadcrumbJsonLd,
  buildSeoMeta,
  buildWebPageJsonLd,
  siteTitle,
} from "@/shared/seo/metadata";
import { loadNotices } from "@/shared/seo/serverApi.server";
import { CspJsonLd } from "@/shared/seo/CspJsonLd";

const TITLE = siteTitle("공지사항");
const DESCRIPTION = "해피갤러리 공방의 공지사항입니다.";

export async function loader({ request }: Route.LoaderArgs) {
  return { notices: await loadNotices(request.signal) };
}

export function meta({ loaderData }: Route.MetaArgs) {
  return buildSeoMeta({
    title: TITLE,
    description: DESCRIPTION,
    pathname: "/notices",
    image: heroWorkshop,
    indexable: Boolean(loaderData),
  });
}

export default function NoticesRoute({ loaderData }: Route.ComponentProps) {
  return (
    <>
      <CspJsonLd value={[
        buildWebPageJsonLd("/notices", TITLE, DESCRIPTION, "CollectionPage"),
        buildBreadcrumbJsonLd([{ name: "공지사항", pathname: "/notices" }]),
      ]} />
      <NoticesPage initialNotices={loaderData.notices} />
    </>
  );
}

export { PublicRouteErrorBoundary as ErrorBoundary } from "@/shared/seo/PublicRouteErrorBoundary";
