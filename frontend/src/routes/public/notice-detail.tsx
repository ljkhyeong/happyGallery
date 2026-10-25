import type { Route } from "./+types/notice-detail";
import heroWorkshop from "@/assets/happygallery/hero-workshop.jpg";
import { NoticeDetailPage } from "@/pages/NoticeDetailPage";
import {
  buildBreadcrumbJsonLd,
  buildSeoMeta,
  buildWebPageJsonLd,
  seoDescription,
  siteTitle,
} from "@/shared/seo/metadata";
import { buildNoticeArticleJsonLd } from "@/shared/seo/schemas";
import { loadNotice, requirePublicId } from "@/shared/seo/serverApi.server";
import { CspJsonLd } from "@/shared/seo/CspJsonLd";

const FALLBACK_DESCRIPTION = "해피갤러리 공방의 클래스와 운영 소식입니다.";

export async function loader({ params, request }: Route.LoaderArgs) {
  const noticeId = requirePublicId(params.id);
  return { notice: await loadNotice(noticeId, request.signal) };
}

/** meta와 화면이 같은 주소·제목·설명을 쓴다. */
function seo({ notice }: Route.ComponentProps["loaderData"]) {
  return {
    pathname: `/notices/${notice.id}`,
    title: siteTitle(notice.title),
    description: seoDescription(notice.content, FALLBACK_DESCRIPTION),
  };
}

export function meta({ loaderData, params }: Route.MetaArgs) {
  const { pathname, title, description } = loaderData ? seo(loaderData) : {
    pathname: `/notices/${params.id ?? ""}`,
    title: siteTitle("공지를 찾을 수 없습니다"),
    description: FALLBACK_DESCRIPTION,
  };

  return buildSeoMeta({
    title,
    description,
    pathname,
    image: heroWorkshop,
    type: "article",
    indexable: Boolean(loaderData),
  });
}

export default function NoticeDetailRoute({ loaderData }: Route.ComponentProps) {
  const { pathname, title, description } = seo(loaderData);
  return (
    <>
      <CspJsonLd value={[
        buildWebPageJsonLd(pathname, title, description),
        buildBreadcrumbJsonLd([
          { name: "공지사항", pathname: "/notices" },
          { name: loaderData.notice.title, pathname },
        ]),
        buildNoticeArticleJsonLd(loaderData.notice, pathname, heroWorkshop),
      ]} />
      <NoticeDetailPage initialNotice={loaderData.notice} />
    </>
  );
}

export { PublicRouteErrorBoundary as ErrorBoundary } from "@/shared/seo/PublicRouteErrorBoundary";
