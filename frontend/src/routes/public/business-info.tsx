import type { Route } from "./+types/business-info";
import heroWorkshop from "@/assets/happygallery/hero-workshop.jpg";
import { BusinessInfoPage } from "@/pages/BusinessInfoPage";
import {
  buildBreadcrumbJsonLd,
  buildSeoMeta,
  buildWebPageJsonLd,
} from "@/shared/seo/metadata";
import { buildLocalBusinessJsonLd } from "@/shared/seo/schemas";
import { loadWorkshop } from "@/shared/seo/serverApi.server";
import { CspJsonLd } from "@/shared/seo/CspJsonLd";

const TITLE = "사업자 정보 | 해피갤러리";
const DESCRIPTION = "해피갤러리 사업자와 공방 정보를 확인하세요.";

export async function loader({ request }: Route.LoaderArgs) {
  return { workshop: await loadWorkshop(request.signal) };
}

export function meta({ loaderData }: Route.MetaArgs) {
  return buildSeoMeta({
    title: TITLE,
    description: DESCRIPTION,
    pathname: "/business-info",
    image: heroWorkshop,
    indexable: Boolean(loaderData),
  });
}

export default function BusinessInfoRoute({ loaderData }: Route.ComponentProps) {
  return (
    <>
      <CspJsonLd value={[
        buildWebPageJsonLd("/business-info", TITLE, DESCRIPTION, "AboutPage"),
        buildBreadcrumbJsonLd([
          { name: "홈", pathname: "/" },
          { name: "사업자 정보", pathname: "/business-info" },
        ]),
        buildLocalBusinessJsonLd(loaderData.workshop, heroWorkshop),
      ]} />
      <BusinessInfoPage initialWorkshop={loaderData.workshop} />
    </>
  );
}

export { PublicRouteErrorBoundary as ErrorBoundary } from "@/shared/seo/PublicRouteErrorBoundary";
