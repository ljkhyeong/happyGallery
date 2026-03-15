import { useState, useCallback } from "react";
import { useMutation } from "@tanstack/react-query";
import { Container, Card, Form, Button, Row, Col, Badge } from "react-bootstrap";
import { Link } from "react-router-dom";
import { fetchOrder } from "@/features/order/api";
import { OrderDetailCard } from "@/features/order/OrderDetailCard";
import { ErrorAlert } from "@/shared/ui";
import type { OrderDetailResponse } from "@/shared/types";

export function OrderDetailPage() {
  const [orderId, setOrderId] = useState("");
  const [token, setToken] = useState("");
  const [order, setOrder] = useState<OrderDetailResponse | null>(null);

  const lookup = useMutation({
    mutationFn: ({ id, t }: { id: number; t: string }) => fetchOrder(id, t),
    onSuccess: setOrder,
    onError: () => setOrder(null),
  });

  const handleLookup = useCallback(() => {
    if (Number(orderId) > 0 && token.trim()) {
      lookup.mutate({ id: Number(orderId), t: token.trim() });
    }
  }, [orderId, token, lookup]);

  return (
    <Container className="page-container" style={{ maxWidth: 640 }}>
      <Card className="legacy-order-banner mb-4 border-0">
        <Card.Body className="p-4">
          <Badge bg="light" text="dark" className="mb-2">Guest Lookup</Badge>
          <h4 className="mb-2">비회원 주문 조회</h4>
          <p className="text-muted-soft mb-3">
            주문 완료 후 받은 주문 ID와 access token으로 주문 상태를 확인합니다.
            회원은 <strong>내 정보</strong>에서 추가 인증 없이 주문을 바로 볼 수 있습니다.
          </p>
          <div className="d-flex flex-wrap gap-2">
            <Button as={Link as any} to="/my" variant="dark" size="sm">
              회원 내 정보
            </Button>
            <Button as={Link as any} to="/signup" variant="outline-secondary" size="sm">
              회원가입
            </Button>
            <Button as={Link as any} to="/products" variant="outline-secondary" size="sm">
              상품 보러가기
            </Button>
          </div>
        </Card.Body>
      </Card>

      <Card className="mb-4">
        <Card.Body>
          <div className="legacy-order-step-label mb-2">주문 ID + 토큰 입력</div>
          <p className="text-muted-soft small mb-3">
            비회원 주문은 발급된 token으로 조회합니다. 이후 회원가입하면 같은 휴대폰 번호 기준으로 이력을 가져올 수 있습니다.
          </p>
          <Form onSubmit={(e) => { e.preventDefault(); handleLookup(); }}>
            <Row className="g-2 align-items-end">
              <Col xs={12} sm={4}>
                <Form.Group controlId="order-detail-id">
                  <Form.Label>주문 ID</Form.Label>
                  <Form.Control type="number" min={1} value={orderId}
                    onChange={(e) => setOrderId(e.target.value)} placeholder="주문 ID" />
                </Form.Group>
              </Col>
              <Col xs={12} sm={5}>
                <Form.Group controlId="order-detail-token">
                  <Form.Label>인증 토큰</Form.Label>
                  <Form.Control value={token}
                    onChange={(e) => setToken(e.target.value)} placeholder="주문 시 발급된 토큰" />
                </Form.Group>
              </Col>
              <Col xs={12} sm={3}>
                <Button type="submit" variant="primary" className="w-100"
                  disabled={!Number(orderId) || !token.trim() || lookup.isPending}>
                  {lookup.isPending ? "조회 중..." : "조회"}
                </Button>
              </Col>
            </Row>
          </Form>
        </Card.Body>
      </Card>

      <ErrorAlert error={lookup.error} />

      {order && <OrderDetailCard order={order} />}
    </Container>
  );
}
