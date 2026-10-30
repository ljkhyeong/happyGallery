import { useEffect } from "react";
import { skipToken, useMutation, useQuery } from "@tanstack/react-query";
import { abandonPayment, fetchPaymentStatus } from "@/features/payment/api";
import {
  isCompletedPayment,
  isEndedWithoutPayment,
  PaymentCompletedCard,
  PaymentResultCard,
  paymentStatusPollingInterval,
  PaymentStatusNotice,
} from "@/features/payment";
import { paymentFailureMessage } from "@/features/payment/paymentFailure";
import { consumePaymentReturnHint, readPaymentReturnHint, readPaymentStatusToken } from "@/features/payment/session";
import { resolveSafeReturnTo } from "@/features/customer-auth/navigation";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { captureCustomerSession, queryKeys, requireCurrentCustomerSession, runForCustomerSession } from "@/shared/api";
import { ErrorAlert } from "@/shared/ui";
import { LinkButton } from "@/shared/ui/LinkButton";
import { Container, Button } from "react-bootstrap";
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
    // 이미 끝난 결제(승인 실패·만료·환불)는 종료할 것이 없어 종료 요청 없이 돌아간다. 서버는 끝난 결제의 종료를 409로 거절한다.
    mutationFn: async (endPendingPayment: boolean) => {
      requireCurrentCustomerSession(customerSession);
      const orderId = returnHint?.value.orderId;
      try {
        if (orderId && endPendingPayment) {
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
    queryKey: queryKeys.member.paymentAbandonStatus(customerSession.version, customerSession.boundaryEpoch, orderId),
    queryFn: returnMutation.isError && orderId
      ? () => runForCustomerSession(customerSession,
        () => fetchPaymentStatus(orderId, readPaymentStatusToken(orderId, customerSession)))
      : skipToken,
    gcTime: 0,
    refetchInterval: ({ state }) => paymentStatusPollingInterval(state.data?.status),
  });

  useEffect(() => {
    window.history.replaceState(window.history.state, "", window.location.pathname);
  }, []);

  // 종료 요청이 거절된 뒤 확인한 실제 결제 상태. 확인 전이거나 복귀 정보가 없으면 결제창에서 끝난 결제로 본다.
  const currentStatus = returnHint ? statusQuery.data : undefined;
  if (currentStatus && isCompletedPayment(currentStatus)) {
    return (
      <Container className="page-container is-narrow">
        <PaymentCompletedCard result={currentStatus} />
      </Container>
    );
  }
  if (currentStatus && !isEndedWithoutPayment(currentStatus.status)) {
    // 승인 확인·환불이 진행 중인 결제는 새 결제로 이어지지 않게 구매 화면 복귀 없이 상태만 보인다.
    return (
      <Container className="page-container is-narrow">
        <PaymentResultCard
          tone="notice"
          title="결제 상태"
          actions={(
            <>
              <Button variant="primary" disabled={statusQuery.isFetching}
                onClick={() => void statusQuery.refetch()}>결제 상태 확인</Button>
              <LinkButton to="/" variant="outline-secondary">홈으로</LinkButton>
            </>
          )}
        >
          <PaymentStatusNotice status={currentStatus} />
        </PaymentResultCard>
      </Container>
    );
  }

  return (
    <Container className="page-container is-narrow">
      <PaymentResultCard
        tone="danger"
        title="결제 실패"
        lead={<>결제가 완료되지 않았습니다.<br />{message}</>}
        actions={(
          <>
            {returnPath !== "/" && (
              <Button variant="primary" disabled={returnMutation.isPending}
                onClick={() => returnMutation.mutate(currentStatus === undefined)}>
                {returnMutation.isPending ? "결제 종료 중…" : "구매 화면으로 돌아가기"}
              </Button>
            )}
            {orderId && returnMutation.isError && !currentStatus && (
              <Button variant="outline-primary" disabled={statusQuery.isFetching}
                onClick={() => void statusQuery.refetch()}>결제 상태 확인</Button>
            )}
            <LinkButton to="/" variant="outline-secondary">홈으로</LinkButton>
            <LinkButton to="/products" variant={returnPath === "/" ? "primary" : "outline-secondary"}>작품 둘러보기</LinkButton>
          </>
        )}
      >
        {/* 승인 실패·만료·환불로 끝난 결제는 종료할 것이 없어 거절 오류와 종료 안내 대신 그 상태를 보인다. */}
        {currentStatus
          ? <PaymentStatusNotice status={currentStatus} />
          : returnHint && <ErrorAlert error={returnMutation.error} />}
        {orderId && !currentStatus && (
          <p className="payment-result-note">구매 화면으로 돌아가면 승인 전 결제를 종료하고 쿠폰·적립금 사용 예약을 해제합니다.</p>
        )}
      </PaymentResultCard>
    </Container>
  );
}
