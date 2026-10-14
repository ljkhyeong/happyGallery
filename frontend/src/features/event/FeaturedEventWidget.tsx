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
          <h2 id="home-event-title">이벤트</h2>
          <p>지금 소개할 추천 이벤트가 없습니다.</p>
          <LinkButton to="/events" variant="outline-light">이벤트 목록</LinkButton>
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
            <Badge bg={isEventOngoing(featured) ? "dark" : "light"} text={isEventOngoing(featured) ? undefined : "dark"}>
              {isEventOngoing(featured) ? "진행 중" : "시작 예정"}
            </Badge>
          </div>
          <h2 id="home-event-title">{featured.title}</h2>
          <p className="home-event-summary">{featured.summary}</p>
          <EventPeriodText event={featured} className="home-event-period" />
          <div className="home-event-actions">
            <LinkButton to={`/events/${featured.id}`} variant="light">이벤트 보기</LinkButton>
            <LinkButton to="/events" variant="outline-light">이벤트 목록</LinkButton>
          </div>
        </div>
      )}
    </section>
  );
}
