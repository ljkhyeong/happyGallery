import { useEffect } from "react";
import { paymentFailureMessage } from "@/features/payment/paymentFailure";
import { LinkButton } from "@/shared/ui/LinkButton";
import { Container, Alert } from "react-bootstrap";
import { useSearchParams } from "react-router";

export function PaymentFailPage() {
  const [params] = useSearchParams();
  const code = params.get("code");
  const message = paymentFailureMessage(code);

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
      <div className="d-flex gap-2">
        <LinkButton to="/" variant="outline-secondary">홈으로</LinkButton>
        <LinkButton to="/products" variant="primary">상품 둘러보기</LinkButton>
      </div>
    </Container>
  );
}
