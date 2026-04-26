import { useEffect, useRef, useState } from "react";
import { Container, Spinner, Button } from "react-bootstrap";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import {
  confirmPayment,
  consumePaymentReturnHint,
  type ConfirmPaymentResponse,
} from "@/features/payment";
import { ErrorAlert } from "@/shared/ui";

export function PaymentSuccessPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const [error, setError] = useState<Error | null>(null);
  const [result, setResult] = useState<ConfirmPaymentResponse | null>(null);
  const calledRef = useRef(false);

  const paymentKey = params.get("paymentKey");
  const orderId = params.get("orderId");
  const amountStr = params.get("amount");
  const amount = amountStr ? Number(amountStr) : null;

  useEffect(() => {
    if (calledRef.current) return;
    calledRef.current = true;

    if (!orderId || amount === null || Number.isNaN(amount)) {
      setError(new Error("결제 정보가 올바르지 않습니다."));
      return;
    }

    confirmPayment({ paymentKey, orderId, amount })
      .then((res) => {
        setResult(res);
        consumePaymentReturnHint();
      })
      .catch((e) => setError(e instanceof Error ? e : new Error(String(e))));
  }, [paymentKey, orderId, amount]);

  if (error) {
    return (
      <Container className="page-container" style={{ maxWidth: 540 }}>
        <h4 className="mb-4">결제 확정 실패</h4>
        <ErrorAlert error={error} />
        <div className="d-flex gap-2 mt-3">
          <Button variant="outline-secondary" onClick={() => navigate("/")}>홈으로</Button>
          <Button variant="primary" onClick={() => navigate(-1)}>이전으로</Button>
        </div>
      </Container>
    );
  }

  if (!result) {
    return (
      <Container className="page-container text-center" style={{ maxWidth: 540 }}>
        <Spinner animation="border" role="status" className="mb-3" />
        <p className="text-muted-soft">결제를 확정하고 있습니다...</p>
      </Container>
    );
  }

  return (
    <Container className="page-container" style={{ maxWidth: 540 }}>
      <h4 className="mb-4">결제 완료</h4>
      <p className="text-muted-soft mb-4">결제가 정상 처리되었습니다.</p>
      <PaymentSuccessNext result={result} />
    </Container>
  );
}

function PaymentSuccessNext({ result }: { result: ConfirmPaymentResponse }) {
  if (result.context === "PASS") {
    return (
      <Button as={Link as any} to="/my/passes" variant="primary">
        내 8회권 확인하기
      </Button>
    );
  }
  if (result.context === "ORDER") {
    if (result.accessToken) {
      return (
        <Button
          as={Link as any}
          to="/guest/orders"
          state={{ orderId: result.domainId, token: result.accessToken }}
          variant="primary"
        >
          비회원 주문 확인하기
        </Button>
      );
    }
    return (
      <Button as={Link as any} to={`/my/orders/${result.domainId}`} variant="primary">
        내 주문 상세 보기
      </Button>
    );
  }
  // BOOKING
  if (result.accessToken) {
    return (
      <Button
        as={Link as any}
        to="/guest/bookings"
        state={{ bookingId: result.domainId, token: result.accessToken }}
        variant="primary"
      >
        비회원 예약 확인하기
      </Button>
    );
  }
  return (
    <Button as={Link as any} to={`/my/bookings/${result.domainId}`} variant="primary">
      내 예약 상세 보기
    </Button>
  );
}
