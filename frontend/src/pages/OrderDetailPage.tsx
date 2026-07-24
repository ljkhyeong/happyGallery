import { LinkButton } from "@/shared/ui/LinkButton";
import { useState } from "react";
import { skipToken, useMutation, useQuery } from "@tanstack/react-query";
import { Container, Card, Form, Button, Row, Col, Badge } from "react-bootstrap";
import { useLocation, useSearchParams } from "react-router-dom";
import { cancelGuestOrder, fetchOrder, respondToGuestOrderDelay } from "@/features/order/api";
import { buildAuthPageHref } from "@/features/customer-auth/navigation";
import { trackGuestMemberCta } from "@/features/monitoring/api";
import { OrderDetailCard } from "@/features/order/OrderDetailCard";
import { OrderCustomerActionPanel } from "@/features/order/OrderCustomerActionPanel";
import { ErrorAlert } from "@/shared/ui";
import { customerRefundPollingInterval } from "@/shared/lib";
import { loadGuestRecordRecovery } from "@/features/guest-recovery/session";
import { OrderClaimSection } from "@/features/order-claim/OrderClaimSection";

interface LocationState {
  orderId?: number;
  token?: string;
}

interface OrderLookup {
  credentials: {
    id: number;
    token: string;
  };
  requestId: string;
}

export function OrderDetailPage() {
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const navState = location.state as LocationState | null;
  const [initialCredentials] = useState(() => {
    const queryOrderId = Number(searchParams.get("orderId"));
    const orderId = navState?.orderId
      ?? (Number.isSafeInteger(queryOrderId) && queryOrderId > 0 ? queryOrderId : undefined);
    const token = navState?.token ?? loadGuestRecordRecovery()?.accessToken ?? "";
    return { orderId, token: token.trim() };
  });
  const [orderId, setOrderId] = useState(
    initialCredentials.orderId ? String(initialCredentials.orderId) : "",
  );
  const [token, setToken] = useState(initialCredentials.token);
  const [lookup, setLookup] = useState<OrderLookup | null>(() =>
    initialCredentials.orderId && initialCredentials.token
      ? {
          credentials: { id: initialCredentials.orderId, token: initialCredentials.token },
          requestId: crypto.randomUUID(),
        }
      : null,
  );
  const parsedOrderId = Number(orderId);
  const validOrderId = Number.isSafeInteger(parsedOrderId) && parsedOrderId > 0;
  const normalizedToken = token.trim();

  const { data: order, error, isFetching, refetch: refetchOrder } = useQuery({
    queryKey: ["guest", "order", lookup?.credentials.id, lookup?.requestId],
    queryFn: lookup
      ? () => fetchOrder(lookup.credentials.id, lookup.credentials.token)
      : skipToken,
    gcTime: 0,
    refetchInterval: ({ state }) =>
      customerRefundPollingInterval(
        state.data?.refund?.status,
        state.dataUpdateCount + state.fetchFailureCount,
      ),
  });
  const cancelMutation = useMutation({
    mutationFn: ({ id, token: accessToken }: { id: number; token: string }) =>
      cancelGuestOrder(id, accessToken),
    onSuccess: () => refetchOrder(),
  });
  const delayMutation = useMutation({
    mutationFn: ({ id, token: accessToken, decision }: {
      id: number;
      token: string;
      decision: "ACCEPT" | "REJECT";
    }) => respondToGuestOrderDelay(id, accessToken, decision),
    onSuccess: () => refetchOrder(),
  });

  function handleLookup() {
    if (validOrderId && normalizedToken) {
      if (
        lookup?.credentials.id === parsedOrderId &&
        lookup.credentials.token === normalizedToken
      ) {
        void refetchOrder();
        return;
      }
      setLookup({
        credentials: { id: parsedOrderId, token: normalizedToken },
        requestId: crypto.randomUUID(),
      });
    }
  }
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
          <Badge bg="light" text="dark" className="mb-2">비회원 주문 관리</Badge>
          <h4 className="mb-2">비회원 주문 조회</h4>
          <p className="text-muted-soft mb-3">
            완료한 비회원 주문과 현재 처리 상태를 확인할 수 있습니다.
            주문을 계속 관리할 계획이면 회원으로 전환해 <strong>내 정보</strong>에서 바로 확인하는 흐름을 권장합니다.
          </p>
          <div className="d-flex flex-wrap gap-2">
            <LinkButton to="/my" variant="dark" size="sm">
              회원 내 정보
            </LinkButton>
            <LinkButton
              to={claimLoginHref}
              variant="outline-secondary"
              size="sm"
              onClick={() => trackGuestMemberCta("guest_order_lookup", "login")}
            >
              로그인하고 가져오기
            </LinkButton>
            <LinkButton
              to={claimSignupHref}
              variant="outline-secondary"
              size="sm"
              onClick={() => trackGuestMemberCta("guest_order_lookup", "signup")}
            >
              회원가입
            </LinkButton>
            <LinkButton to="/products" variant="outline-secondary" size="sm">
              상품 보러가기
            </LinkButton>
          </div>
          <div className="guest-route-note mt-3">
            <div className="guest-route-note-title">조회 안내</div>
            <div className="small text-muted-soft">
              비회원 주문은 조회 코드로 확인하고, 회원가입 후에는 같은 번호의 이력을 내 정보로 가져와 계속 관리할 수 있습니다.
            </div>
          </div>
        </Card.Body>
      </Card>

      <Card className="mb-4">
        <Card.Body>
          <div className="legacy-order-step-label mb-2">주문 번호와 조회 코드 입력</div>
          <p className="text-muted-soft small mb-3">
            주문 완료 후 받은 조회 코드로 확인합니다. 이후 회원가입하면 같은 휴대폰 번호의 이력을 가져올 수 있습니다.
          </p>
          <Form onSubmit={(e) => { e.preventDefault(); handleLookup(); }}>
            <Row className="g-2 align-items-end">
              <Col xs={12} sm={4}>
                <Form.Group controlId="order-detail-id">
                  <Form.Label>주문 번호</Form.Label>
                  <Form.Control type="number" min={1} value={orderId}
                    onChange={(e) => setOrderId(e.target.value)} placeholder="주문 번호" />
                </Form.Group>
              </Col>
              <Col xs={12} sm={5}>
                <Form.Group controlId="order-detail-token">
                  <Form.Label>조회 코드</Form.Label>
                  <Form.Control value={token}
                    onChange={(e) => setToken(e.target.value)} placeholder="주문 시 발급된 조회 코드" />
                </Form.Group>
              </Col>
              <Col xs={12} sm={3}>
                <Button type="submit" variant="primary" className="w-100"
                  disabled={!validOrderId || !normalizedToken || isFetching}>
                  {isFetching ? "조회 중..." : "조회"}
                </Button>
              </Col>
            </Row>
          </Form>
        </Card.Body>
      </Card>

      <ErrorAlert error={order ? null : error} />

      <ErrorAlert error={cancelMutation.error ?? delayMutation.error} />
      {order && (
        <>
          <OrderDetailCard order={order} />
          <OrderCustomerActionPanel
            status={order.status}
            pending={cancelMutation.isPending || delayMutation.isPending}
            onCancel={() => lookup && cancelMutation.mutate(lookup.credentials)}
            onDelayDecision={(decision) => lookup && delayMutation.mutate({
              ...lookup.credentials,
              decision,
            })}
          />
          {lookup && (
            <OrderClaimSection
              order={order}
              access={{
                kind: "guest",
                accessToken: lookup.credentials.token,
                requestKey: lookup.requestId,
              }}
            />
          )}
        </>
      )}
    </Container>
  );
}
