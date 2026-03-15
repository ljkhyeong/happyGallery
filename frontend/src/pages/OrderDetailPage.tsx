import { useState, useCallback } from "react";
import { useMutation } from "@tanstack/react-query";
import { Container, Card, Form, Button, Row, Col } from "react-bootstrap";
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
      <h4 className="mb-4">주문 조회 <small className="text-muted-soft">(비회원)</small></h4>
      <p className="text-muted-soft small mb-3">
        회원이신가요? <a href="/my">내 정보</a>에서 주문을 확인하세요.
      </p>

      <Card className="mb-4">
        <Card.Body>
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
