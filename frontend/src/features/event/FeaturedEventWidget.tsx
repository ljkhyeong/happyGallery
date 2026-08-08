import { useQuery } from "@tanstack/react-query";
import { Badge, Container } from "react-bootstrap";
import { Link } from "react-router";
import { fetchEvents } from "./api";
import { isEventAvailable, isEventOngoing } from "./time";
import { queryKeys } from "@/shared/api";
import { PUBLIC_DATA_STALE_TIME } from "@/shared/api/staleTimes";
import { formatDateTime } from "@/shared/lib";
import { ErrorAlert, LoadingSpinner } from "@/shared/ui";

export function FeaturedEventWidget() {
  const eventsQuery = useQuery({
    queryKey: queryKeys.events.all,
    queryFn: fetchEvents,
    staleTime: PUBLIC_DATA_STALE_TIME,
  });
  const featuredEvents = eventsQuery.data?.filter(
    (event) => event.featured && isEventAvailable(event),
  ) ?? [];
  const featured = featuredEvents.find((event) => isEventOngoing(event)) ?? featuredEvents[0];

  if (!eventsQuery.isLoading && !eventsQuery.error && !featured) return null;

  return (
    <section className="home-band anim-fade-up">
      <Container>
        {eventsQuery.isLoading && <LoadingSpinner text="이벤트를 불러오는 중입니다" />}
        <ErrorAlert
          error={eventsQuery.error}
          onRetry={() => void eventsQuery.refetch()}
          retrying={eventsQuery.isFetching}
        />
        {featured && (
          <div className="rounded-4 overflow-hidden bg-dark text-white shadow-sm">
            <div className="row g-0 align-items-stretch">
              {featured.imageUrl && (
                <div className="col-12 col-lg-6">
                  <img
                    src={featured.imageUrl}
                    alt={`${featured.title} 대표 이미지`}
                    className="w-100 h-100"
                    style={{ minHeight: 280, objectFit: "cover" }}
                  />
                </div>
              )}
              <div className={featured.imageUrl ? "col-12 col-lg-6" : "col-12"}>
                <div className="p-4 p-md-5 h-100 d-flex flex-column justify-content-center">
                  <div className="d-flex flex-wrap gap-2 mb-3">
                    <Badge bg={isEventOngoing(featured) ? "success" : "light"} text={isEventOngoing(featured) ? undefined : "dark"}>
                      {isEventOngoing(featured) ? "지금 진행 중" : "곧 시작해요"}
                    </Badge>
                    <span className="small text-white-50">Featured event</span>
                  </div>
                  <h2 className="mb-3">{featured.title}</h2>
                  <p className="lead mb-3">{featured.summary}</p>
                  <p className="small text-white-50 mb-4">
                    {formatDateTime(featured.startAt)} ~ {formatDateTime(featured.endAt)}
                  </p>
                  <div className="d-flex flex-wrap gap-3">
                    <Link to={`/events/${featured.id}`} className="btn btn-light">
                      이벤트 보기
                    </Link>
                    <Link to="/events" className="btn btn-outline-light">
                      전체 이벤트
                    </Link>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}
      </Container>
    </section>
  );
}
