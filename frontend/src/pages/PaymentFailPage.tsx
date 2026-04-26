import { Container, Alert, Button } from "react-bootstrap";
import { Link, useSearchParams } from "react-router-dom";

export function PaymentFailPage() {
  const [params] = useSearchParams();
  const code = params.get("code");
  const message = params.get("message");

  return (
    <Container className="page-container" style={{ maxWidth: 540 }}>
      <h4 className="mb-4">결제 실패</h4>
      <Alert variant="danger" className="mb-3">
        <Alert.Heading className="h6 mb-2">결제가 완료되지 않았습니다.</Alert.Heading>
        <p className="mb-1 small">{message ?? "Toss 결제창에서 결제가 취소되었거나 처리에 실패했습니다."}</p>
        {code && <p className="text-muted small mb-0">코드: {code}</p>}
      </Alert>
      <div className="d-flex gap-2">
        <Button as={Link as any} to="/" variant="outline-secondary">홈으로</Button>
        <Button as={Link as any} to="/products" variant="primary">상품 둘러보기</Button>
      </div>
    </Container>
  );
}
