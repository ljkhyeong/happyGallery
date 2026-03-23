import { Card, Col, Row } from "react-bootstrap";
import { Link } from "react-router-dom";
import type { MyOrderSummary } from "./api";
import { LoadingSpinner, ErrorAlert, EmptyState, StatusBadge } from "@/shared/ui";
import { formatKRW, formatDateTime } from "@/shared/lib";

interface Props {
  orders: MyOrderSummary[] | undefined;
  isLoading: boolean;
  error: Error | null;
  totalCount: number;
}

export function MyOrdersSection({ orders, isLoading, error, totalCount }: Props) {
  return (
    <section id="my-orders" className="mb-4">
      <div className="d-flex justify-content-between align-items-center mb-2">
        <div>
          <h6 className="mb-1">내 주문</h6>
          <p className="text-muted-soft small mb-0">최근 5건의 주문 진행 상태를 빠르게 확인합니다.</p>
        </div>
        <div className="d-flex align-items-center gap-3">
          <span className="text-muted-soft small">총 {totalCount}건</span>
          <Link to="/my/orders" className="my-inline-link small">전체 보기</Link>
        </div>
      </div>
      {isLoading && <LoadingSpinner />}
      <ErrorAlert error={error} />
      {orders && orders.length === 0 && <EmptyState message="주문 내역이 없습니다." />}
      {orders && orders.length > 0 && orders.slice(0, 5).map((o) => (
        <Card key={o.orderId} as={Link} to={`/my/orders/${o.orderId}`} className="mb-2 text-decoration-none my-list-card border-0">
          <Card.Body className="py-3 px-3">
            <Row className="align-items-center g-2">
              <Col xs={12} md={4}>
                <div className="fw-semibold small">주문 #{o.orderId}</div>
                <small className="text-muted-soft">
                  {o.paidAt ? `결제 ${formatDateTime(o.paidAt)}` : formatDateTime(o.createdAt)}
                </small>
              </Col>
              <Col xs={6} md={3}>
                <StatusBadge status={o.status} />
              </Col>
              <Col xs={6} md={5} className="text-md-end">
                <small>{formatKRW(o.totalAmount)}</small>
              </Col>
            </Row>
          </Card.Body>
        </Card>
      ))}
      {orders && orders.length > 5 && (
        <p className="text-muted-soft small mt-2 mb-0">최근 5건만 표시합니다.</p>
      )}
    </section>
  );
}
