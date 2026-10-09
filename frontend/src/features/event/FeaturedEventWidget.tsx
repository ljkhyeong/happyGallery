import { Badge } from "react-bootstrap";
import { publicEventsQuery } from "./api";
import { EventPeriodText } from "./EventPeriodText";
import { isEventAvailable, isEventOngoing } from "./time";
import { useLoaderBackedQuery } from "@/shared/api";
import { ErrorAlert, LinkButton } from "@/shared/ui";
import type { EventResponse } from "./api";

export function FeaturedEventWidget({ initialEvents }: { initialEvents: EventResponse[] }) {
  const {
    data: events,
    error,
    query: eventsQuery,
  } = useLoaderBackedQuery(publicEventsQuery(), initialEvents);
  const featuredEvents = events.filter(
    (event) => event.featured && isEventAvailable(event),
  );
  const featured = featuredEvents.find((event) => isEventOngoing(event)) ?? featuredEvents[0];

  return (
    <section
      className={`home-update-panel home-event-panel${featured?.imageUrl ? " has-media" : ""}`}
      aria-labelledby="home-event-title"
    >
      {error && (
        <div className="home-event-state">
          <ErrorAlert
            error={error}
            onRetry={() => void eventsQuery.refetch()}
            retrying={eventsQuery.isFetching}
          />
        </div>
      )}
      {!error && !featured && (
        <div className="home-event-copy home-event-empty">
          <p className="store-section-kicker">이벤트</p>
          <h2 id="home-event-title">새로운 이벤트를 준비하고 있습니다</h2>
          <p>진행 예정인 공방 소식은 이벤트 목록에서 확인할 수 있습니다.</p>
          <LinkButton to="/events" variant="outline-light">전체 이벤트</LinkButton>
        </div>
      )}
      {featured?.imageUrl && (
        <figure className="home-event-media">
          <img src={featured.imageUrl} alt={`${featured.title} 대표 이미지`} />
        </figure>
      )}
      {featured && (
        <div className="home-event-copy">
          <div className="home-event-labels">
            <p className="store-section-kicker">추천 이벤트</p>
            <Badge bg={isEventOngoing(featured) ? "success" : "light"} text={isEventOngoing(featured) ? undefined : "dark"}>
              {isEventOngoing(featured) ? "지금 진행 중" : "곧 시작해요"}
            </Badge>
          </div>
          <h2 id="home-event-title">{featured.title}</h2>
          <p className="home-event-summary">{featured.summary}</p>
          <EventPeriodText event={featured} className="home-event-period" />
          <div className="home-event-actions">
            <LinkButton to={`/events/${featured.id}`} variant="light">이벤트 보기</LinkButton>
            <LinkButton to="/events" variant="outline-light">전체 이벤트</LinkButton>
          </div>
        </div>
      )}
    </section>
  );
}
