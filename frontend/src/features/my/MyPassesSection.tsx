import { LinkButton } from "@/shared/ui/LinkButton";
import { Card, Col, Row } from "react-bootstrap";
import { Link } from "react-router";
import type { MyPassSummary } from "./api";
import { isPassAvailableForBooking } from "./listUtils";
import { LoadingSpinner, ErrorAlert, EmptyState } from "@/shared/ui";
import { formatDateTime } from "@/shared/lib";

interface Props {
  passes: MyPassSummary[] | undefined;
  isLoading: boolean;
  error: Error | null;
  isFetching: boolean;
  onRetry: () => void;
}

export function MyPassesSection({
  passes,
  isLoading,
  error,
  isFetching,
  onRetry,
}: Props) {
  return (
    <section id="my-passes">
      <div className="d-flex justify-content-between align-items-center mb-2">
        <div>
          <h6 className="mb-1">내 8회권</h6>
          <p className="text-muted-soft small mb-0">남은 횟수와 만료일을 기준으로 현재 사용 가능한 8회권을 확인합니다.</p>
        </div>
        <div className="d-flex align-items-center gap-3">
          {passes && <span className="text-muted-soft small">총 {passes.length}건</span>}
          <Link to="/my/passes" className="my-inline-link small">전체 보기</Link>
        </div>
      </div>
      {isLoading && <LoadingSpinner />}
      <ErrorAlert error={error} onRetry={onRetry} retrying={isFetching} />
      {passes && passes.length === 0 && <EmptyState message="8회권이 없습니다." />}
      {passes && passes.length > 0 && passes.map((p) => (
        <Card key={p.passId} className="mb-2 my-list-card border-0">
          <Card.Body className="py-3 px-3">
            <Row className="align-items-center g-2">
              <Col xs={12} md={4}>
                <div className="fw-semibold small">{p.planName} #{p.passId}</div>
                <small className="text-muted-soft">구매 {formatDateTime(p.purchasedAt)}</small>
              </Col>
              <Col xs={6} md={4}>
                <small>잔여 <strong>{p.remainingCredits}</strong>/{p.totalCredits}회</small>
              </Col>
              <Col xs={6} md={4} className="text-md-end">
                <small className="d-block text-muted-soft">~{formatDateTime(p.expiresAt)}</small>
                {isPassAvailableForBooking(p) && (
                  <LinkButton
                    to={`/bookings/new?passId=${p.passId}`}
                    variant="outline-primary"
                    size="sm"
                    className="mt-2"
                  >
                    이 8회권으로 예약
                  </LinkButton>
                )}
              </Col>
            </Row>
          </Card.Body>
        </Card>
      ))}
    </section>
  );
}
