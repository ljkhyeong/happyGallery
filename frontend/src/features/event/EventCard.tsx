import { Badge, Card } from "react-bootstrap";
import { Link } from "react-router";
import type { EventResponse } from "./api";
import { eventTimingLabel } from "./time";
import { formatDateTime } from "@/shared/lib";

export function EventCard({ event }: { event: EventResponse }) {
  const timing = eventTimingLabel(event);

  return (
    <Card className="h-100 border-0 shadow-sm overflow-hidden">
      {event.imageUrl && (
        <Card.Img
          variant="top"
          src={event.imageUrl}
          alt={`${event.title} 대표 이미지`}
          style={{ aspectRatio: "16 / 9", objectFit: "cover" }}
        />
      )}
      <Card.Body className="d-flex flex-column gap-3">
        <div className="d-flex flex-wrap align-items-center gap-2">
          <Badge bg={timing === "진행 중" ? "success" : "secondary"}>{timing}</Badge>
          {event.featured && <Badge bg="dark">추천</Badge>}
        </div>
        <div>
          <Card.Title as="h2" className="h5">{event.title}</Card.Title>
          <Card.Text className="text-muted-soft mb-0">{event.summary}</Card.Text>
        </div>
        <p className="small text-muted-soft mb-0">
          {formatDateTime(event.startAt)} ~ {formatDateTime(event.endAt)}
        </p>
        <Link to={`/events/${event.id}`} className="stretched-link mt-auto text-decoration-none">
          자세히 보기 <span aria-hidden="true">→</span>
        </Link>
      </Card.Body>
    </Card>
  );
}
