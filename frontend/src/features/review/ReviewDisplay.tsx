import { Badge } from "react-bootstrap";
import { formatDateTime } from "@/shared/lib";

export function ReviewStars({ rating }: { rating: number }) {
  return (
    <span className="review-stars" aria-label={`별점 ${rating}점`}>
      <span aria-hidden="true">{"★".repeat(rating)}{"☆".repeat(5 - rating)}</span>
    </span>
  );
}

export function ReviewStatusBadge({ status }: { status: "PUBLISHED" | "HIDDEN" }) {
  return status === "PUBLISHED"
    ? <Badge bg="success">공개</Badge>
    : <Badge bg="secondary">숨김</Badge>;
}

export function ReviewDate({ value }: { value: string }) {
  return <time dateTime={value}>{formatDateTime(value)}</time>;
}
