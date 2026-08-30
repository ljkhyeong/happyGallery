import type { Route } from "./+types/events";
import heroWorkshop from "@/assets/happygallery/hero-workshop.jpg";
import { EventsPage } from "@/pages/EventsPage";
import {
  buildBreadcrumbJsonLd,
  buildSeoMeta,
  buildWebPageJsonLd,
} from "@/shared/seo/metadata";
import { loadEvents } from "@/shared/seo/serverApi.server";
import { CspJsonLd } from "@/shared/seo/CspJsonLd";

const TITLE = "이벤트 | 해피갤러리";
const DESCRIPTION = "해피갤러리에서 진행 중이거나 곧 시작할 이벤트를 확인하세요.";

export async function loader({ request }: Route.LoaderArgs) {
  return { events: await loadEvents(request.signal) };
}

export function meta({ loaderData }: Route.MetaArgs) {
  return buildSeoMeta({
    title: TITLE,
    description: DESCRIPTION,
    pathname: "/events",
    image: heroWorkshop,
    indexable: Boolean(loaderData),
  });
}

export default function EventsRoute({ loaderData }: Route.ComponentProps) {
  return (
    <>
      <CspJsonLd value={[
        buildWebPageJsonLd("/events", TITLE, DESCRIPTION, "CollectionPage"),
        buildBreadcrumbJsonLd([
          { name: "홈", pathname: "/" },
          { name: "이벤트", pathname: "/events" },
        ]),
      ]} />
      <EventsPage initialEvents={loaderData.events} />
    </>
  );
}

export { PublicRouteErrorBoundary as ErrorBoundary } from "@/shared/seo/PublicRouteErrorBoundary";
