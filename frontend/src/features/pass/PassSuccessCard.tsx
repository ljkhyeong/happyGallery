import { Card, Alert } from "react-bootstrap";
import { Link } from "react-router-dom";
import { formatKRW, formatDate } from "@/shared/lib";
import type { PurchasePassResponse } from "@/shared/types";

interface Props {
  pass: PurchasePassResponse;
}

export function PassSuccessCard({ pass }: Props) {
  return (
    <div>
      <Alert variant="success" className="mb-3">
        8회권 구매가 완료되었습니다!
      </Alert>
      <Card>
        <Card.Body>
          <p className="mb-1">
            <strong>8회권 ID:</strong> {pass.passId}
          </p>
          <p className="mb-1">
            <strong>총 횟수:</strong> {pass.totalCredits}회
          </p>
          <p className="mb-1">
            <strong>잔여 횟수:</strong> {pass.remainingCredits}회
          </p>
          <p className="mb-1">
            <strong>결제 금액:</strong> {formatKRW(pass.totalPrice)}
          </p>
          <p className="mb-0">
            <strong>만료일:</strong> {formatDate(pass.expiresAt)}
          </p>
        </Card.Body>
        <Card.Footer className="small">
          <p className="mb-1 text-muted-soft">
            예약 시 8회권 ID <strong>{pass.passId}</strong>를 입력하면 횟수가 차감됩니다.
          </p>
          <Link to="/bookings/new" className="text-decoration-none">
            예약하러 가기 &rarr;
          </Link>
        </Card.Footer>
      </Card>
    </div>
  );
}
