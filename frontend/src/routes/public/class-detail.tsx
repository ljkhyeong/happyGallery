import type { Route } from "./+types/class-detail";
import leatherClass from "@/assets/happygallery/leather-class.jpg";
import { ClassDetailPage } from "@/pages/ClassDetailPage";
import {
  buildBreadcrumbJsonLd,
  buildSeoMeta,
  buildWebPageJsonLd,
  seoDescription,
  siteTitle,
} from "@/shared/seo/metadata";
import { buildCourseJsonLd } from "@/shared/seo/schemas";
import { loadClass, requirePublicId } from "@/shared/seo/serverApi.server";
import { CspJsonLd } from "@/shared/seo/CspJsonLd";

const FALLBACK_DESCRIPTION = "해피갤러리 공예 클래스의 수업 정보와 후기, 예약할 수 있는 일정입니다.";

export async function loader({ params, request }: Route.LoaderArgs) {
  const classId = requirePublicId(params.id);
  return { bookingClass: await loadClass(classId, request.signal) };
}

/** meta와 화면이 같은 주소·제목·설명을 쓴다. */
function seo({ bookingClass }: Route.ComponentProps["loaderData"]) {
  return {
    pathname: `/classes/${bookingClass.id}`,
    title: siteTitle(bookingClass.name),
    description: seoDescription(bookingClass.description, FALLBACK_DESCRIPTION),
  };
}

export function meta({ loaderData, params }: Route.MetaArgs) {
  const { pathname, title, description } = loaderData ? seo(loaderData) : {
    pathname: `/classes/${params.id ?? ""}`,
    title: siteTitle("클래스를 찾을 수 없습니다"),
    description: FALLBACK_DESCRIPTION,
  };

  return buildSeoMeta({
    title,
    description,
    pathname,
    image: loaderData?.bookingClass.imageUrl ?? leatherClass,
    indexable: Boolean(loaderData),
  });
}

export default function ClassDetailRoute({ loaderData }: Route.ComponentProps) {
  const { pathname, title, description } = seo(loaderData);
  return (
    <>
      <CspJsonLd value={[
        buildWebPageJsonLd(pathname, title, description),
        buildBreadcrumbJsonLd([
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
