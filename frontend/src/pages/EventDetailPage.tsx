import { useMemo } from "react";
import { Badge, Container } from "react-bootstrap";
import { Link } from "react-router";
import { fetchEvent, type EventResponse } from "@/features/event/api";
import { EventCouponClaim } from "@/features/event/EventCouponClaim";
import { eventRefetchInterval, eventTimingLabel } from "@/features/event/time";
import { ApiError, queryKeys, useLoaderBackedQuery } from "@/shared/api";
import { formatDateTime } from "@/shared/lib";
import { ErrorAlert, LoadingSpinner } from "@/shared/ui";
import { NotFoundPage } from "@/pages/NotFoundPage";

export function EventDetailPage({ initialEvent }: { initialEvent: EventResponse }) {
  const eventId = initialEvent.id;
  const eventQueryKey = useMemo(
    () => queryKeys.events.detail(eventId),
    [eventId],
  );
  const {
    data: event,
    error,
    isLoading,
    query: eventQuery,
  } = useLoaderBackedQuery({
    queryKey: eventQueryKey,
    queryFn: ({ signal }) => fetchEvent(eventId, signal),
    retry: (failureCount, error) => !isNotFoundError(error) && failureCount < 3,
    refetchInterval: ({ state }) => state.error
      ? false
      : eventRefetchInterval(state.data ? [state.data] : undefined),
  }, initialEvent);

  if (isNotFoundError(error)) return <NotFoundPage />;

  return (
    <Container className="page-container" style={{ maxWidth: 900 }}>
      <Link to="/events" className="text-decoration-none small text-muted-soft d-inline-block mb-3">
        &larr; 이벤트 목록
      </Link>

      {isLoading && <LoadingSpinner text="이벤트를 불러오는 중입니다" />}
      <ErrorAlert
        error={error}
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

          {event.couponDefinitionId !== null && (
            <EventCouponClaim
              eventId={event.id}
              couponDefinitionId={event.couponDefinitionId}
            />
          )}

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

function isNotFoundError(error: unknown): boolean {
  return error instanceof ApiError && error.status === 404;
}
