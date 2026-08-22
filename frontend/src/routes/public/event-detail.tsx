import type { Route } from "./+types/event-detail";
import heroWorkshop from "@/assets/happygallery/hero-workshop.jpg";
import { EventDetailPage } from "@/pages/EventDetailPage";
import {
  buildBreadcrumbJsonLd,
  buildSeoMeta,
  buildWebPageJsonLd,
  seoDescription,
} from "@/shared/seo/metadata";
import { loadEvent, requirePublicId } from "@/shared/seo/serverApi.server";
import { CspJsonLd } from "@/shared/seo/CspJsonLd";

const FALLBACK_DESCRIPTION = "해피갤러리 이벤트 일정과 참여 내용을 확인하세요.";

export async function loader({ params, request }: Route.LoaderArgs) {
  const eventId = requirePublicId(params.id);
  return { event: await loadEvent(eventId, request.signal) };
}

export function meta({ loaderData, params }: Route.MetaArgs) {
  const pathname = `/events/${params.id ?? ""}`;
  const title = loaderData ? `${loaderData.event.title} | 해피갤러리` : "이벤트를 찾을 수 없습니다 | 해피갤러리";
  const description = loaderData
    ? seoDescription(loaderData.event.summary, FALLBACK_DESCRIPTION)
    : FALLBACK_DESCRIPTION;

  return buildSeoMeta({
    title,
    description,
    pathname,
    image: loaderData?.event.imageUrl ?? heroWorkshop,
    indexable: Boolean(loaderData),
  });
}

export default function EventDetailRoute({ loaderData }: Route.ComponentProps) {
  const pathname = `/events/${loaderData.event.id}`;
  const title = `${loaderData.event.title} | 해피갤러리`;
  const description = seoDescription(loaderData.event.summary, FALLBACK_DESCRIPTION);
  return (
    <>
      <CspJsonLd value={[
        buildWebPageJsonLd(pathname, title, description),
        buildBreadcrumbJsonLd([
          { name: "홈", pathname: "/" },
          { name: "이벤트", pathname: "/events" },
          { name: loaderData.event.title, pathname },
        ]),
      ]} />
      <EventDetailPage initialEvent={loaderData.event} />
    </>
  );
}

export { PublicRouteErrorBoundary as ErrorBoundary } from "@/shared/seo/PublicRouteErrorBoundary";
