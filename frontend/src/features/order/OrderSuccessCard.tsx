import { Card, Alert } from "react-bootstrap";
import { Link } from "react-router-dom";
import { StatusBadge } from "@/shared/ui";
import { formatKRW } from "@/shared/lib";
import type { OrderResponse } from "@/shared/types";

interface Props {
  order: OrderResponse;
}

export function OrderSuccessCard({ order }: Props) {
  return (
    <div>
      <Alert variant="success" className="mb-3">주문이 완료되었습니다!</Alert>
      <Card>
        <Card.Body>
          <div className="d-flex justify-content-between align-items-center mb-3">
            <h5 className="mb-0">주문 #{order.orderId}</h5>
            <StatusBadge status={order.status} />
          </div>
          <p className="mb-1"><strong>결제 금액:</strong> {formatKRW(order.totalAmount)}</p>
          <Alert variant="warning" className="mt-3 mb-0">
            <small>
              <strong>Access Token:</strong> <code>{order.accessToken}</code>
              <br />이 토큰은 주문 조회에 필요합니다. 반드시 보관하세요.
            </small>
          </Alert>
        </Card.Body>
        <Card.Footer>
          <Link to="/orders/detail" className="text-decoration-none small">
            주문 조회 페이지로 이동 &rarr;
          </Link>
        </Card.Footer>
      </Card>
    </div>
  );
}
