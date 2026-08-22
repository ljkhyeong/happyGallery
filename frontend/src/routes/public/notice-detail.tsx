import type { Route } from "./+types/notice-detail";
import heroWorkshop from "@/assets/happygallery/hero-workshop.jpg";
import { NoticeDetailPage } from "@/pages/NoticeDetailPage";
import {
  buildBreadcrumbJsonLd,
  buildSeoMeta,
  buildWebPageJsonLd,
  seoDescription,
} from "@/shared/seo/metadata";
import { buildNoticeArticleJsonLd } from "@/shared/seo/schemas";
import { loadNotice, requirePublicId } from "@/shared/seo/serverApi.server";
import { CspJsonLd } from "@/shared/seo/CspJsonLd";

const FALLBACK_DESCRIPTION = "해피갤러리의 클래스와 공방 운영 소식을 확인하세요.";

export async function loader({ params, request }: Route.LoaderArgs) {
  const noticeId = requirePublicId(params.id);
  return { notice: await loadNotice(noticeId, request.signal) };
}

export function meta({ loaderData, params }: Route.MetaArgs) {
  const pathname = `/notices/${params.id ?? ""}`;
  const title = loaderData ? `${loaderData.notice.title} | 해피갤러리` : "공지를 찾을 수 없습니다 | 해피갤러리";
  const description = loaderData
    ? seoDescription(loaderData.notice.content, FALLBACK_DESCRIPTION)
    : FALLBACK_DESCRIPTION;

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
  const pathname = `/notices/${loaderData.notice.id}`;
  const title = `${loaderData.notice.title} | 해피갤러리`;
  const description = seoDescription(loaderData.notice.content, FALLBACK_DESCRIPTION);
  return (
    <>
      <CspJsonLd value={[
        buildWebPageJsonLd(pathname, title, description),
        buildBreadcrumbJsonLd([
          { name: "홈", pathname: "/" },
          { name: loaderData.notice.title, pathname },
        ]),
        buildNoticeArticleJsonLd(loaderData.notice, pathname, heroWorkshop),
      ]} />
      <NoticeDetailPage initialNotice={loaderData.notice} />
    </>
  );
}

export { PublicRouteErrorBoundary as ErrorBoundary } from "@/shared/seo/PublicRouteErrorBoundary";
