import type { Route } from "./+types/classes";
import leatherClass from "@/assets/happygallery/leather-class.jpg";
import { ClassListPage } from "@/pages/ClassListPage";
import {
  buildBreadcrumbJsonLd,
  buildSeoMeta,
  buildWebPageJsonLd,
} from "@/shared/seo/metadata";
import { loadClasses } from "@/shared/seo/serverApi.server";
import { CspJsonLd } from "@/shared/seo/CspJsonLd";

const TITLE = "충주 공예 원데이클래스 | 해피갤러리";
const DESCRIPTION = "가죽공예, 레진아트, 위빙 등 해피갤러리 공예 수업의 가격과 예약 방법을 확인하세요.";

export async function loader({ request }: Route.LoaderArgs) {
  return { classes: await loadClasses(request.signal) };
}

export function meta({ loaderData }: Route.MetaArgs) {
  return buildSeoMeta({
    title: TITLE,
    description: DESCRIPTION,
    pathname: "/classes",
    image: leatherClass,
    indexable: Boolean(loaderData),
  });
}

export default function ClassesRoute({ loaderData }: Route.ComponentProps) {
  return (
    <>
      <CspJsonLd value={[
        buildWebPageJsonLd("/classes", TITLE, DESCRIPTION, "CollectionPage"),
        buildBreadcrumbJsonLd([
          { name: "홈", pathname: "/" },
          { name: "클래스", pathname: "/classes" },
        ]),
      ]} />
      <ClassListPage initialClasses={loaderData.classes} />
    </>
  );
}

export { PublicRouteErrorBoundary as ErrorBoundary } from "@/shared/seo/PublicRouteErrorBoundary";
