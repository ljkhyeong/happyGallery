import type { Route } from "./+types/group-classes";
import groupResinClass from "@/assets/happygallery/group-resin-class.jpg";
import { GroupClassesPage } from "@/pages/GroupClassesPage";
import {
  buildBreadcrumbJsonLd,
  buildSeoMeta,
  buildWebPageJsonLd,
} from "@/shared/seo/metadata";
import { loadWorkshop } from "@/shared/seo/serverApi.server";
import { CspJsonLd } from "@/shared/seo/CspJsonLd";

const TITLE = "충주 단체·기관 공예수업 | 해피갤러리";
const DESCRIPTION = "학교, 기관, 모임을 위한 충주 해피갤러리 출장·단체 공예수업을 문의하세요.";

export async function loader({ request }: Route.LoaderArgs) {
  return { workshop: await loadWorkshop(request.signal) };
}

export function meta({ loaderData }: Route.MetaArgs) {
  return buildSeoMeta({
    title: TITLE,
    description: DESCRIPTION,
    pathname: "/group-classes",
    image: groupResinClass,
    indexable: Boolean(loaderData),
  });
}

export default function GroupClassesRoute({ loaderData }: Route.ComponentProps) {
  return (
    <>
      <CspJsonLd value={[
        buildWebPageJsonLd("/group-classes", TITLE, DESCRIPTION),
        buildBreadcrumbJsonLd([
          { name: "홈", pathname: "/" },
          { name: "단체·기관 수업", pathname: "/group-classes" },
        ]),
      ]} />
      <GroupClassesPage initialWorkshop={loaderData.workshop} />
    </>
  );
}

export { PublicRouteErrorBoundary as ErrorBoundary } from "@/shared/seo/PublicRouteErrorBoundary";
