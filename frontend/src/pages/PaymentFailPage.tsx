import { useEffect } from "react";
import { paymentFailureMessage } from "@/features/payment/paymentFailure";
import { consumePaymentReturnHint, readPaymentReturnHint } from "@/features/payment/session";
import { resolveSafeReturnTo } from "@/features/customer-auth/navigation";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { captureCustomerSession, isCurrentCustomerSession } from "@/shared/api";
import { LinkButton } from "@/shared/ui/LinkButton";
import { Container, Alert } from "react-bootstrap";
import { useSearchParams } from "react-router";

export function PaymentFailPage() {
  const [params] = useSearchParams();
  const code = params.get("code");
  const message = paymentFailureMessage(code);
  const { status } = useCustomerAuth();
  const customerSession = captureCustomerSession();
  const returnHint = status === "authenticated" || status === "unauthenticated"
    ? readPaymentReturnHint(customerSession)
    : null;
  const returnPath = resolveSafeReturnTo(returnHint?.value.returnPath);

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
      <div className="d-flex flex-wrap gap-2">
        {returnPath !== "/" && (
          <LinkButton to={returnPath} variant="primary" onClick={(event) => {
            if (!isCurrentCustomerSession(customerSession)) {
              event.preventDefault();
              return;
            }
            if (returnHint) consumePaymentReturnHint(returnHint);
          }}>구매 화면으로 돌아가기</LinkButton>
        )}
        <LinkButton to="/" variant="outline-secondary">홈으로</LinkButton>
        <LinkButton to="/products" variant={returnPath === "/" ? "primary" : "outline-secondary"}>상품 둘러보기</LinkButton>
      </div>
    </Container>
  );
}
