import type { Route } from "./+types/class-detail";
import leatherClass from "@/assets/happygallery/leather-class.jpg";
import { ClassDetailPage } from "@/pages/ClassDetailPage";
import {
  buildBreadcrumbJsonLd,
  buildSeoMeta,
  buildWebPageJsonLd,
  seoDescription,
} from "@/shared/seo/metadata";
import { buildCourseJsonLd } from "@/shared/seo/schemas";
import { loadClass, requirePublicId } from "@/shared/seo/serverApi.server";
import { CspJsonLd } from "@/shared/seo/CspJsonLd";

const FALLBACK_DESCRIPTION = "해피갤러리 공예 클래스의 수업 정보와 이용 후기를 확인하고 원하는 날짜를 예약하세요.";

export async function loader({ params, request }: Route.LoaderArgs) {
  const classId = requirePublicId(params.id);
  return { bookingClass: await loadClass(classId, request.signal) };
}

export function meta({ loaderData, params }: Route.MetaArgs) {
  const pathname = `/classes/${params.id ?? ""}`;
  const title = loaderData ? `${loaderData.bookingClass.name} | 해피갤러리` : "클래스를 찾을 수 없습니다 | 해피갤러리";
  const description = loaderData
    ? seoDescription(loaderData.bookingClass.description, FALLBACK_DESCRIPTION)
    : FALLBACK_DESCRIPTION;

  return buildSeoMeta({
    title,
    description,
    pathname,
    image: loaderData?.bookingClass.imageUrl ?? leatherClass,
    indexable: Boolean(loaderData),
  });
}

export default function ClassDetailRoute({ loaderData }: Route.ComponentProps) {
  const pathname = `/classes/${loaderData.bookingClass.id}`;
  const title = `${loaderData.bookingClass.name} | 해피갤러리`;
  const description = seoDescription(loaderData.bookingClass.description, FALLBACK_DESCRIPTION);
  return (
    <>
      <CspJsonLd value={[
        buildWebPageJsonLd(pathname, title, description),
        buildBreadcrumbJsonLd([
          { name: "홈", pathname: "/" },
          { name: "클래스", pathname: "/classes" },
          { name: loaderData.bookingClass.name, pathname },
        ]),
        buildCourseJsonLd(loaderData.bookingClass, pathname, FALLBACK_DESCRIPTION),
      ]} />
      <ClassDetailPage initialClass={loaderData.bookingClass} />
    </>
  );
}

export { PublicRouteErrorBoundary as ErrorBoundary } from "@/shared/seo/PublicRouteErrorBoundary";
