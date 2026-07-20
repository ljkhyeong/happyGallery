import { LinkButton } from "@/shared/ui/LinkButton";
import { useCallback, useEffect, useRef, useState } from "react";
import { Container, Spinner, Button } from "react-bootstrap";
import { useNavigate, useSearchParams } from "react-router-dom";
import {
  confirmPayment,
  consumePaymentReturnHint,
  type ConfirmPaymentResponse,
} from "@/features/payment";
import { isPositiveSafeIntegerString } from "@/shared/lib";
import { ErrorAlert } from "@/shared/ui";
import { ApiError } from "@/shared/api";

function canRetryConfirm(error: unknown): boolean {
  if (error instanceof ApiError) {
    return error.code === "PAYMENT_CONFIRM_IN_PROGRESS"
      || error.code === "PAYMENT_CONFIRM_RETRYABLE"
      || error.code === "SERVICE_UNAVAILABLE";
  }
  return error instanceof TypeError
    || (error instanceof Error && error.name === "AbortError");
}

function requiresPaymentReconciliation(error: unknown): boolean {
  return error instanceof ApiError
    && error.code === "PAYMENT_RECONCILIATION_REQUIRED";
}

export function PaymentSuccessPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const [error, setError] = useState<unknown>(null);
  const [result, setResult] = useState<ConfirmPaymentResponse | null>(null);
  const [confirming, setConfirming] = useState(true);
  const calledRef = useRef(false);

  const paymentKey = params.get("paymentKey")?.trim() ?? "";
  const orderId = params.get("orderId")?.trim() ?? "";
  const amountStr = params.get("amount");
  const amount = Number(amountStr);
  const validAmount = isPositiveSafeIntegerString(amountStr);

  const runConfirm = useCallback(async () => {
    if (!paymentKey || !orderId || !validAmount) {
      setError(new Error("결제 정보가 올바르지 않습니다."));
      setConfirming(false);
      return;
    }

    setError(null);
    setConfirming(true);
    try {
      const response = await confirmPayment({ paymentKey, orderId, amount });
      setResult(response);
      consumePaymentReturnHint();
    } catch (requestError) {
      setError(requestError);
    } finally {
      setConfirming(false);
    }
  }, [paymentKey, orderId, amount, validAmount]);

  useEffect(() => {
    if (calledRef.current) return;
    calledRef.current = true;
    void runConfirm();
  }, [runConfirm]);

  if (confirming) {
    return (
      <Container className="page-container text-center" style={{ maxWidth: 540 }}>
        <Spinner animation="border" role="status" className="mb-3" />
        <p className="text-muted-soft">결제를 확정하고 있습니다...</p>
      </Container>
    );
  }

  if (error) {
    const retryable = canRetryConfirm(error);
    const reconciliationRequired = requiresPaymentReconciliation(error);
    const statusCheckRequired = retryable || reconciliationRequired;
    return (
      <Container className="page-container" style={{ maxWidth: 540 }}>
        <h4 className="mb-4">{statusCheckRequired ? "결제 상태 확인 필요" : "결제 확정 실패"}</h4>
        <ErrorAlert error={error} />
        <div className="d-flex gap-2 mt-3">
          {retryable && (
            <Button variant="primary" onClick={() => void runConfirm()}>
              다시 확인
            </Button>
          )}
          <Button variant="outline-secondary" onClick={() => navigate("/")}>홈으로</Button>
          {!retryable && !reconciliationRequired && (
            <Button variant="primary" onClick={() => navigate(-1)}>이전으로</Button>
          )}
        </div>
      </Container>
    );
  }

  if (!result) {
    return null;
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
      <LinkButton to="/my/passes" variant="primary">
        내 8회권 확인하기
      </LinkButton>
    );
  }
  if (result.context === "ORDER") {
    if (result.accessToken) {
      return (
        <LinkButton
          to="/guest/orders"
          state={{ orderId: result.domainId, token: result.accessToken }}
          variant="primary"
        >
          비회원 주문 확인하기
        </LinkButton>
      );
    }
    return (
      <LinkButton to={`/my/orders/${result.domainId}`} variant="primary">
        내 주문 상세 보기
      </LinkButton>
    );
  }
  // BOOKING
  if (result.accessToken) {
    return (
      <LinkButton
        to="/guest/bookings"
        state={{ bookingId: result.domainId, token: result.accessToken }}
        variant="primary"
      >
        비회원 예약 확인하기
      </LinkButton>
    );
  }
  return (
    <LinkButton to={`/my/bookings/${result.domainId}`} variant="primary">
      내 예약 상세 보기
    </LinkButton>
  );
}
