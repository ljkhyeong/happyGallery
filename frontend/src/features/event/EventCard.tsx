import { Badge, Card } from "react-bootstrap";
import { Link } from "react-router";
import type { EventResponse } from "./api";
import { EventPeriodText } from "./EventPeriodText";
import { eventTimingLabel } from "./time";

export function EventCard({ event }: { event: EventResponse }) {
  const timing = eventTimingLabel(event);

  return (
    <Card className="event-card h-100 overflow-hidden">
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
          <Badge bg={timing === "진행 중" ? "dark" : "light"}>{timing}</Badge>
          {event.featured && <Badge bg="light" text="dark">추천</Badge>}
        </div>
        <div>
          <Card.Title as="h2" className="h5 display-heading">{event.title}</Card.Title>
          <Card.Text className="text-muted-soft mb-0">{event.summary}</Card.Text>
        </div>
        <EventPeriodText event={event} className="small text-muted-soft mb-0" />
        {/* 글자 링크라 밑줄로 보이고, 카드마다 같은 글자라 화면 낭독기에는 이벤트 제목을 함께 읽힌다. */}
        <Link to={`/events/${event.id}`} className="stretched-link mt-auto" aria-label={`${event.title} 자세히 보기`}>
          자세히 보기
        </Link>
      </Card.Body>
    </Card>
  );
}
