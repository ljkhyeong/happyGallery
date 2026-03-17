import { useState, useCallback, useEffect, useRef } from "react";
import { useMutation } from "@tanstack/react-query";
import { Container, Card, Form, Button, Row, Col, Badge } from "react-bootstrap";
import { Link, useLocation } from "react-router-dom";
import { fetchOrder } from "@/features/order/api";
import { buildAuthPageHref } from "@/features/customer-auth/navigation";
import { trackGuestMemberCta } from "@/features/monitoring/api";
import { OrderDetailCard } from "@/features/order/OrderDetailCard";
import { ErrorAlert } from "@/shared/ui";
import type { OrderDetailResponse } from "@/shared/types";

interface LocationState {
  orderId?: number;
  token?: string;
}

export function OrderDetailPage() {
  const location = useLocation();
  const navState = location.state as LocationState | null;
  const [orderId, setOrderId] = useState(navState?.orderId ? String(navState.orderId) : "");
  const [token, setToken] = useState(navState?.token ?? "");
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

  const autoLookupDone = useRef(false);
  useEffect(() => {
    if (!autoLookupDone.current && navState?.orderId && navState?.token) {
      autoLookupDone.current = true;
      lookup.mutate({ id: navState.orderId, t: navState.token });
    }
  }, [navState, lookup]);
  const claimLoginHref = buildAuthPageHref("/login", {
    redirectTo: "/my?claim=1",
    claim: true,
  });
  const claimSignupHref = buildAuthPageHref("/signup", {
    redirectTo: "/my?claim=1",
    claim: true,
  });

  return (
    <Container className="page-container" style={{ maxWidth: 640 }}>
      <Card className="legacy-order-banner mb-4 border-0">
        <Card.Body className="p-4">
          <Badge bg="light" text="dark" className="mb-2">Guest Lookup</Badge>
          <h4 className="mb-2">비회원 주문 조회</h4>
          <p className="text-muted-soft mb-3">
            이 경로는 이미 완료한 비회원 주문을 확인하는 보조 조회 경로입니다.
            주문을 계속 관리할 계획이면 회원으로 전환해 <strong>내 정보</strong>에서 바로 확인하는 흐름을 권장합니다.
          </p>
          <div className="d-flex flex-wrap gap-2">
            <Button as={Link as any} to="/my" variant="dark" size="sm">
              회원 내 정보
            </Button>
            <Button
              as={Link as any}
              to={claimLoginHref}
              variant="outline-secondary"
              size="sm"
              onClick={() => trackGuestMemberCta("guest_order_lookup", "login")}
            >
              로그인하고 가져오기
            </Button>
            <Button
              as={Link as any}
              to={claimSignupHref}
              variant="outline-secondary"
              size="sm"
              onClick={() => trackGuestMemberCta("guest_order_lookup", "signup")}
            >
              회원가입
            </Button>
            <Button as={Link as any} to="/products" variant="outline-secondary" size="sm">
              상품 보러가기
            </Button>
          </div>
          <div className="guest-route-note mt-3">
            <div className="guest-route-note-title">Guest route policy</div>
            <div className="small text-muted-soft">
              비회원 주문은 토큰으로 조회하고, 회원 전환 후에는 같은 번호 기준으로 `/my`에서 이력을 가져와 계속 관리할 수 있습니다.
            </div>
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
