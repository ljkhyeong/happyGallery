import { Col, Container, Row } from "react-bootstrap";
import { EventCard } from "@/features/event/EventCard";
import { fetchEvents } from "@/features/event/api";
import { eventRefetchInterval, isEventAvailable } from "@/features/event/time";
import { queryKeys, useLoaderBackedQuery } from "@/shared/api";
import { PUBLIC_DATA_STALE_TIME } from "@/shared/api/staleTimes";
import { EmptyState, ErrorAlert, LoadingSpinner } from "@/shared/ui";
import type { EventResponse } from "@/features/event/api";

export function EventsPage({ initialEvents }: { initialEvents: EventResponse[] }) {
  const {
    data: loadedEvents,
    error,
    isLoading,
    query: eventsQuery,
  } = useLoaderBackedQuery({
    queryKey: queryKeys.events.all,
    queryFn: ({ signal }) => fetchEvents(signal),
    staleTime: PUBLIC_DATA_STALE_TIME,
    refetchInterval: ({ state }) => eventRefetchInterval(state.data),
  }, initialEvents);
  const events = loadedEvents?.filter((event) => isEventAvailable(event));

  return (
    <Container className="page-container" style={{ maxWidth: 1100 }}>
      <header className="store-section-header mb-4">
        <div>
          <p className="store-section-kicker mb-2">Events</p>
          <h1 className="store-section-title">이벤트</h1>
          <p className="store-section-desc mb-0">
            지금 참여할 수 있는 행사와 앞으로 열릴 소식을 확인하세요.
          </p>
        </div>
      </header>

      {isLoading && <LoadingSpinner text="이벤트를 불러오는 중입니다" />}
      <ErrorAlert
        error={error}
        onRetry={() => void eventsQuery.refetch()}
        retrying={eventsQuery.isFetching}
      />
      {!isLoading && events?.length === 0 && (
        <EmptyState message="현재 안내할 이벤트가 없습니다." />
      )}
      {events && events.length > 0 && (
        <Row xs={1} md={2} className="g-4">
          {events.map((event) => (
            <Col key={event.id}><EventCard event={event} /></Col>
          ))}
        </Row>
      )}
    </Container>
  );
}
