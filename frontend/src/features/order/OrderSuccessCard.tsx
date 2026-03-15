import { Card, Alert } from "react-bootstrap";
import { GuestClaimSuccessActions } from "@/features/customer-claim/GuestClaimSuccessActions";
import { StatusBadge } from "@/shared/ui";
import { formatKRW } from "@/shared/lib";
import type { OrderResponse } from "@/shared/types";

interface Props {
  order: OrderResponse;
  guestPhone?: string;
  guestName?: string;
}

export function OrderSuccessCard({ order, guestPhone, guestName }: Props) {
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
          <GuestClaimSuccessActions
            primaryTo="/guest/orders"
            primaryLabel="비회원 주문 조회"
            guestPhone={guestPhone}
            guestName={guestName}
            helperText="같은 휴대폰 번호로 회원가입하거나 로그인하면 `/my`에서 비회원 주문 이력을 바로 가져올 수 있습니다."
          />
        </Card.Footer>
      </Card>
    </div>
  );
}
