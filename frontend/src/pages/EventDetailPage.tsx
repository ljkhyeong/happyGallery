import { useQuery } from "@tanstack/react-query";
import { Badge, Container } from "react-bootstrap";
import { Link, useParams } from "react-router";
import { fetchEvent } from "@/features/event/api";
import { eventTimingLabel } from "@/features/event/time";
import { queryKeys } from "@/shared/api";
import { formatDateTime, isPositiveSafeIntegerString } from "@/shared/lib";
import { ErrorAlert, LoadingSpinner } from "@/shared/ui";
import { NotFoundPage } from "@/pages/NotFoundPage";

export function EventDetailPage() {
  const { id } = useParams<{ id: string }>();
  const eventId = Number(id);
  const validEventId = isPositiveSafeIntegerString(id);
  const eventQuery = useQuery({
    queryKey: queryKeys.events.detail(eventId),
    queryFn: () => fetchEvent(eventId),
    enabled: validEventId,
  });

  if (!validEventId) return <NotFoundPage />;

  const event = eventQuery.data;

  return (
    <Container className="page-container" style={{ maxWidth: 900 }}>
      <Link to="/events" className="text-decoration-none small text-muted-soft d-inline-block mb-3">
        &larr; 이벤트 목록
      </Link>

      {eventQuery.isLoading && <LoadingSpinner text="이벤트를 불러오는 중입니다" />}
      <ErrorAlert
        error={eventQuery.error}
        onRetry={() => void eventQuery.refetch()}
        retrying={eventQuery.isFetching}
      />

      {event && (
        <article>
          {event.imageUrl && (
            <img
              src={event.imageUrl}
              alt={`${event.title} 대표 이미지`}
              className="w-100 rounded-4 mb-4"
              style={{ maxHeight: 520, objectFit: "cover" }}
            />
          )}
          <div className="d-flex flex-wrap align-items-center gap-2 mb-3">
            <Badge bg={eventTimingLabel(event) === "진행 중" ? "success" : "secondary"}>
              {eventTimingLabel(event)}
            </Badge>
            {event.featured && <Badge bg="dark">추천</Badge>}
          </div>
          <h1 className="mb-3">{event.title}</h1>
          <p className="lead text-muted-soft">{event.summary}</p>
          <p className="small text-muted-soft">
            {formatDateTime(event.startAt)} ~ {formatDateTime(event.endAt)}
          </p>
          <hr className="my-4" />
          <div style={{ whiteSpace: "pre-wrap", lineHeight: 1.85 }}>{event.content}</div>

          {event.relatedProductIds.length > 0 && (
            <section className="mt-5 pt-4 border-top">
              <h2 className="h5 mb-3">함께 보면 좋은 작품</h2>
              <div className="d-flex flex-wrap gap-2">
                {event.relatedProductIds.map((productId: number) => (
                  <Link
                    key={productId}
                    to={`/products/${productId}`}
                    className="btn btn-sm btn-outline-dark"
                  >
                    작품 #{productId}
                  </Link>
                ))}
              </div>
            </section>
          )}
        </article>
      )}
    </Container>
  );
}
