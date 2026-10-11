import { Col, Container, Row } from "react-bootstrap";
import { EventCard } from "@/features/event/EventCard";
import { publicEventsQuery } from "@/features/event/api";
import { isEventAvailable } from "@/features/event/time";
import { useLoaderBackedQuery } from "@/shared/api";
import { EmptyState, ErrorAlert, PageHeader } from "@/shared/ui";
import type { EventResponse } from "@/features/event/api";

export function EventsPage({ initialEvents }: { initialEvents: EventResponse[] }) {
  const {
    data: loadedEvents,
    error,
    query: eventsQuery,
  } = useLoaderBackedQuery(publicEventsQuery(), initialEvents);
  const events = loadedEvents.filter((event) => isEventAvailable(event));

  return (
    <Container className="page-container" style={{ maxWidth: 1100 }}>
      <PageHeader
        title="이벤트"
        description="진행 중이거나 시작 예정인 이벤트입니다."
      />

      <ErrorAlert
        error={error}
        onRetry={() => void eventsQuery.refetch()}
        retrying={eventsQuery.isFetching}
      />
      {events.length === 0 && (
        <EmptyState message="현재 안내할 이벤트가 없습니다." />
      )}
      {events.length > 0 && (
        <Row xs={1} md={2} className="g-4">
          {events.map((event) => (
            <Col key={event.id}><EventCard event={event} /></Col>
          ))}
        </Row>
      )}
    </Container>
  );
}
