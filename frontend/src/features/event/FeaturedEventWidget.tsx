import { Badge } from "react-bootstrap";
import { Link } from "react-router";
import { fetchEvents } from "./api";
import { eventRefetchInterval, isEventAvailable, isEventOngoing } from "./time";
import { queryKeys, useLoaderBackedQuery } from "@/shared/api";
import { PUBLIC_DATA_STALE_TIME } from "@/shared/api/staleTimes";
import { formatDateTime } from "@/shared/lib";
import { ErrorAlert, LoadingSpinner } from "@/shared/ui";
import type { EventResponse } from "./api";

export function FeaturedEventWidget({ initialEvents }: { initialEvents: EventResponse[] }) {
  const {
    data: events,
    error,
    isLoading,
    query: eventsQuery,
  } = useLoaderBackedQuery({
    queryKey: queryKeys.events.all,
    queryFn: ({ signal }) => fetchEvents(signal),
    staleTime: PUBLIC_DATA_STALE_TIME,
    refetchInterval: ({ state }) => eventRefetchInterval(state.data),
  }, initialEvents);
  const featuredEvents = events?.filter(
    (event) => event.featured && isEventAvailable(event),
  ) ?? [];
  const featured = featuredEvents.find((event) => isEventOngoing(event)) ?? featuredEvents[0];

  return (
    <section
      className={`home-update-panel home-event-panel${featured?.imageUrl ? " has-media" : ""}`}
      aria-labelledby="home-event-title"
    >
      {isLoading && (
        <div className="home-event-state">
          <LoadingSpinner text="이벤트를 불러오는 중입니다" />
        </div>
      )}
      {error && (
        <div className="home-event-state">
          <ErrorAlert
            error={error}
            onRetry={() => void eventsQuery.refetch()}
            retrying={eventsQuery.isFetching}
          />
        </div>
      )}
      {!isLoading && !error && !featured && (
        <div className="home-event-copy home-event-empty">
          <p className="store-section-kicker">Event</p>
          <h2 id="home-event-title">새로운 이벤트를 준비하고 있습니다</h2>
          <p>진행 예정인 공방 소식은 이벤트 목록에서 확인할 수 있습니다.</p>
          <Link to="/events" className="btn btn-outline-light">전체 이벤트</Link>
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
            <p className="store-section-kicker">Featured event</p>
            <Badge bg={isEventOngoing(featured) ? "success" : "light"} text={isEventOngoing(featured) ? undefined : "dark"}>
              {isEventOngoing(featured) ? "지금 진행 중" : "곧 시작해요"}
            </Badge>
          </div>
          <h2 id="home-event-title">{featured.title}</h2>
          <p className="home-event-summary">{featured.summary}</p>
          <p className="home-event-period">
            <time dateTime={featured.startAt}>{formatDateTime(featured.startAt)}</time>
            <span aria-hidden="true"> — </span>
            <time dateTime={featured.endAt}>{formatDateTime(featured.endAt)}</time>
          </p>
          <div className="home-event-actions">
            <Link to={`/events/${featured.id}`} className="btn btn-light">이벤트 보기</Link>
            <Link to="/events" className="btn btn-outline-light">전체 이벤트</Link>
          </div>
        </div>
      )}
    </section>
  );
}
