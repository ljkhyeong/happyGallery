import type { Route } from "./+types/event-detail";
import heroWorkshop from "@/assets/happygallery/hero-workshop.jpg";
import { EventDetailPage } from "@/pages/EventDetailPage";
import {
  buildBreadcrumbJsonLd,
  buildSeoMeta,
  buildWebPageJsonLd,
  seoDescription,
  siteTitle,
} from "@/shared/seo/metadata";
import { loadEvent, loadProduct, requirePublicId } from "@/shared/seo/serverApi.server";
import { CspJsonLd } from "@/shared/seo/CspJsonLd";

const FALLBACK_DESCRIPTION = "해피갤러리 이벤트의 일정과 참여 방법입니다.";

export async function loader({ params, request }: Route.LoaderArgs) {
  const eventId = requirePublicId(params.id);
  const event = await loadEvent(eventId, request.signal);
  // 비공개·삭제된 관련 상품이나 상품 조회 장애는 이벤트 본문을 막지 않고 카드만 생략한다.
  const relatedProducts = await Promise.allSettled(
    event.relatedProductIds.map((productId) => loadProduct(productId, request.signal)),
  );
  return {
    event,
    relatedProducts: relatedProducts.flatMap((result) => (
      result.status === "fulfilled" ? [result.value] : []
    )),
  };
}

/** meta와 화면이 같은 주소·제목·설명을 쓴다. */
function seo({ event }: Route.ComponentProps["loaderData"]) {
  return {
    pathname: `/events/${event.id}`,
    title: siteTitle(event.title),
    description: seoDescription(event.summary, FALLBACK_DESCRIPTION),
  };
}

export function meta({ loaderData, params }: Route.MetaArgs) {
  const { pathname, title, description } = loaderData ? seo(loaderData) : {
    pathname: `/events/${params.id ?? ""}`,
    title: siteTitle("이벤트를 찾을 수 없습니다"),
    description: FALLBACK_DESCRIPTION,
  };

  return buildSeoMeta({
    title,
    description,
    pathname,
    image: loaderData?.event.imageUrl ?? heroWorkshop,
    indexable: Boolean(loaderData),
  });
}

export default function EventDetailRoute({ loaderData }: Route.ComponentProps) {
  const { pathname, title, description } = seo(loaderData);
  return (
    <>
      <CspJsonLd value={[
        buildWebPageJsonLd(pathname, title, description),
        buildBreadcrumbJsonLd([
          { name: "이벤트", pathname: "/events" },
          { name: loaderData.event.title, pathname },
        ]),
      ]} />
      <EventDetailPage
        initialEvent={loaderData.event}
        relatedProducts={loaderData.relatedProducts}
      />
    </>
  );
}

export { PublicRouteErrorBoundary as ErrorBoundary } from "@/shared/seo/PublicRouteErrorBoundary";
