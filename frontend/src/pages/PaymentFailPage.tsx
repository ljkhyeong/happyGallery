import { useEffect } from "react";
import { skipToken, useMutation, useQuery } from "@tanstack/react-query";
import { abandonPayment, fetchPaymentStatus } from "@/features/payment/api";
import { PaymentStatusNotice, PaymentCompletionNext, shouldPollPaymentStatus } from "@/features/payment";
import { paymentFailureMessage } from "@/features/payment/paymentFailure";
import { consumePaymentReturnHint, readPaymentReturnHint, readPaymentStatusToken } from "@/features/payment/session";
import { resolveSafeReturnTo } from "@/features/customer-auth/navigation";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { captureCustomerSession, requireCurrentCustomerSession, runForCustomerSession } from "@/shared/api";
import { ErrorAlert } from "@/shared/ui";
import { LinkButton } from "@/shared/ui/LinkButton";
import { Container, Alert, Button } from "react-bootstrap";
import { useNavigate, useSearchParams } from "react-router";

export function PaymentFailPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const code = params.get("code");
  const message = paymentFailureMessage(code);
  const { status } = useCustomerAuth();
  const customerSession = captureCustomerSession();
  const returnHint = status === "authenticated" || status === "unauthenticated"
    ? readPaymentReturnHint(customerSession)
    : null;
  const returnPath = resolveSafeReturnTo(returnHint?.value.returnPath);
  const returnMutation = useMutation({
    mutationFn: async () => {
      requireCurrentCustomerSession(customerSession);
      const orderId = returnHint?.value.orderId;
      try {
        if (orderId) {
          await abandonPayment(orderId, readPaymentStatusToken(orderId, customerSession));
        }
      } finally {
        requireCurrentCustomerSession(customerSession);
      }
      if (returnHint) consumePaymentReturnHint(returnHint);
      navigate(returnPath);
    },
  });
  const orderId = returnHint?.value.orderId;
  const statusQuery = useQuery({
    queryKey: ["me", "payment-abandon-status", customerSession.version, customerSession.boundaryEpoch, orderId],
    queryFn: returnMutation.isError && orderId
      ? () => runForCustomerSession(customerSession,
        () => fetchPaymentStatus(orderId, readPaymentStatusToken(orderId, customerSession)))
      : skipToken,
    gcTime: 0,
    refetchInterval: ({ state }) => shouldPollPaymentStatus(state.data?.status) ? 3000 : false,
  });

  useEffect(() => {
    window.history.replaceState(window.history.state, "", window.location.pathname);
  }, []);

  return (
    <Container className="page-container" style={{ maxWidth: 540 }}>
      <h4 className="mb-4">결제 실패</h4>
      <Alert variant="danger" className="mb-3">
        <Alert.Heading className="h6 mb-2">결제가 완료되지 않았습니다.</Alert.Heading>
        <p className="mb-0 small">{message}</p>
      </Alert>
      {returnHint && <ErrorAlert error={returnMutation.error} />}
      {returnHint && statusQuery.data && (
        <div className="mb-3">
          <PaymentStatusNotice status={statusQuery.data} />
          {statusQuery.data.status === "COMPLETED" && statusQuery.data.domainId != null && (
            <div className="mt-2"><PaymentCompletionNext result={{ ...statusQuery.data, domainId: statusQuery.data.domainId }} /></div>
          )}
        </div>
      )}
      {returnHint?.value.orderId && (
        <p className="small text-muted">구매 화면으로 돌아가면 승인 전 결제를 종료하고 쿠폰·적립금 사용 예약을 해제합니다.</p>
      )}
      <div className="d-flex flex-wrap gap-2">
        {returnPath !== "/" && (
          <Button variant="primary" disabled={returnMutation.isPending}
            onClick={() => returnMutation.mutate()}>
            {returnMutation.isPending ? "결제 종료 중…" : "구매 화면으로 돌아가기"}
          </Button>
        )}
        {orderId && returnMutation.isError && (
          <Button variant="outline-primary" disabled={statusQuery.isFetching}
            onClick={() => void statusQuery.refetch()}>결제 상태 확인</Button>
        )}
        <LinkButton to="/" variant="outline-secondary">홈으로</LinkButton>
        <LinkButton to="/products" variant={returnPath === "/" ? "primary" : "outline-secondary"}>상품 둘러보기</LinkButton>
      </div>
    </Container>
  );
}
