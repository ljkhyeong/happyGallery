import { Card, Col, Row } from "react-bootstrap";
import { Link } from "react-router-dom";
import type { MyPassSummary } from "./api";
import { LoadingSpinner, ErrorAlert, EmptyState } from "@/shared/ui";
import { formatDateTime } from "@/shared/lib";

interface Props {
  passes: MyPassSummary[] | undefined;
  isLoading: boolean;
  error: Error | null;
  totalCount: number;
}

export function MyPassesSection({ passes, isLoading, error, totalCount }: Props) {
  return (
    <section id="my-passes">
      <div className="d-flex justify-content-between align-items-center mb-2">
        <div>
          <h6 className="mb-1">내 8회권</h6>
          <p className="text-muted-soft small mb-0">남은 횟수와 만료일을 기준으로 현재 사용 가능한 8회권을 확인합니다.</p>
        </div>
        <div className="d-flex align-items-center gap-3">
          <span className="text-muted-soft small">총 {totalCount}건</span>
          <Link to="/my/passes" className="my-inline-link small">전체 보기</Link>
        </div>
      </div>
      {isLoading && <LoadingSpinner />}
      <ErrorAlert error={error} />
      {passes && passes.length === 0 && <EmptyState message="8회권이 없습니다." />}
      {passes && passes.length > 0 && passes.map((p) => (
        <Card key={p.passId} className="mb-2 my-list-card border-0">
          <Card.Body className="py-3 px-3">
            <Row className="align-items-center g-2">
              <Col xs={12} md={4}>
                <div className="fw-semibold small">8회권 #{p.passId}</div>
                <small className="text-muted-soft">구매 {formatDateTime(p.purchasedAt)}</small>
              </Col>
              <Col xs={6} md={4}>
                <small>잔여 <strong>{p.remainingCredits}</strong>/{p.totalCredits}회</small>
              </Col>
              <Col xs={6} md={4} className="text-md-end">
                <small className="text-muted-soft">~{formatDateTime(p.expiresAt)}</small>
              </Col>
            </Row>
          </Card.Body>
        </Card>
      ))}
    </section>
  );
}
