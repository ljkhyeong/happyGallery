import { useState } from "react";
import { skipToken, useMutation, useQuery } from "@tanstack/react-query";
import { Button, Container } from "react-bootstrap";
import { useLocation, useSearchParams } from "react-router";
import { cancelGuestOrder, fetchOrder, respondToGuestOrderDelay } from "@/features/order/api";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { GuestLookupForm } from "@/features/guest-lookup/GuestLookupForm";
import { GuestLookupPanel } from "@/features/guest-lookup/GuestLookupPanel";
import { GuestMemberGuide } from "@/features/guest-lookup/GuestMemberGuide";
import { OrderDetailCard } from "@/features/order/OrderDetailCard";
import { ShippingAddressEditPanel } from "@/features/order/ShippingAddressEditPanel";
import { OrderCustomerActionPanel } from "@/features/order/OrderCustomerActionPanel";
import { ErrorAlert } from "@/shared/ui";
import { customerRefundPollingInterval } from "@/shared/lib";
import { loadGuestRecordRecovery } from "@/features/guest-recovery/session";
import { OrderClaimSection } from "@/features/order-claim/OrderClaimSection";
import {
  isCurrentCustomerSessionState,
  runForCurrentCustomer,
  type CustomerSessionOwnedState,
} from "@/shared/api";

interface LocationState extends CustomerSessionOwnedState {
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
  const { sessionVersion } = useCustomerAuth();
  return <OrderDetailContent key={sessionVersion} />;
}

function OrderDetailContent() {
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const navState = isCurrentCustomerSessionState(location.state)
    ? location.state as LocationState
    : null;
  const [initialCredentials] = useState(() => {
    const queryOrderId = Number(searchParams.get("orderId"));
    const orderId = navState?.orderId
      ?? (Number.isSafeInteger(queryOrderId) && queryOrderId > 0 ? queryOrderId : undefined);
    const token = navState?.token
      ?? loadGuestRecordRecovery()?.value.accessToken
      ?? "";
    return { orderId, token: token.trim() };
  });
  const [lookup, setLookup] = useState<OrderLookup | null>(() =>
    initialCredentials.orderId && initialCredentials.token
      ? {
          credentials: { id: initialCredentials.orderId, token: initialCredentials.token },
          requestId: crypto.randomUUID(),
        }
      : null,
  );
  const { data: order, error, isFetching, refetch: refetchOrder } = useQuery({
    queryKey: ["guest", "order", lookup?.credentials.id, lookup?.requestId],
    queryFn: lookup
      ? () => runForCurrentCustomer(
          () => fetchOrder(lookup.credentials.id, lookup.credentials.token),
        )
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
      runForCurrentCustomer(
        () => cancelGuestOrder(id, accessToken),
        async (result, requireCurrent) => {
          requireCurrent();
          await refetchOrder();
          requireCurrent();
          return result;
        },
      ),
  });
  const delayMutation = useMutation({
    mutationFn: ({ id, token: accessToken, decision }: {
      id: number;
      token: string;
      decision: "ACCEPT" | "REJECT";
    }) => runForCurrentCustomer(
      () => respondToGuestOrderDelay(id, accessToken, decision),
      async (result, requireCurrent) => {
        requireCurrent();
        await refetchOrder();
        requireCurrent();
        return result;
      },
    ),
  });

  const [relookupOpen, setRelookupOpen] = useState(false);

  function handleLookup(id: number, token: string) {
    setRelookupOpen(false);
    if (lookup?.credentials.id === id && lookup.credentials.token === token) {
      void refetchOrder();
      return;
    }
    setLookup({ credentials: { id, token }, requestId: crypto.randomUUID() });
  }

  return (
    <Container className="page-container guest-lookup-page">
      {Boolean(order) && (
        <header className="guest-detail-header">
          <div>
            <p className="store-section-kicker mb-1">Guest order</p>
            <h1>비회원 주문 조회</h1>
            <p>주문 상태와 수령 정보를 확인하세요. 조회 코드는 다른 사람과 공유하지 마세요.</p>
          </div>
          <div className="guest-detail-actions">
            <Button variant="outline-dark" size="sm" disabled={isFetching} onClick={() => void refetchOrder()}>
              {isFetching ? "확인 중..." : "최신 상태 확인"}
            </Button>
            <Button
              variant="outline-dark"
              size="sm"
              aria-expanded={relookupOpen}
              aria-controls="guest-relookup"
              onClick={() => setRelookupOpen((open) => !open)}
            >
              {relookupOpen ? "다른 번호 입력 닫기" : "다른 번호로 찾기"}
            </Button>
          </div>
        </header>
      )}
      {/* 조회 후에도 입력값은 유지해야 하므로 지우지 않고 숨긴다. */}
      <div id="guest-relookup" hidden={Boolean(order) && !relookupOpen}>
        <GuestLookupPanel title="비회원 주문 조회" kind="orders">
          <GuestLookupForm kind="orders" onLookup={handleLookup} isLoading={isFetching}
            initialId={initialCredentials.orderId ? String(initialCredentials.orderId) : undefined}
            initialToken={initialCredentials.token} submitLabel="조회" />
        </GuestLookupPanel>
      </div>

      <ErrorAlert error={order ? null : error} />

      <ErrorAlert error={cancelMutation.error ?? delayMutation.error} />
      {order && (
        <>
          <OrderDetailCard order={order} />
          {lookup && <ShippingAddressEditPanel key={lookup.requestId} order={order} accessToken={lookup.credentials.token} onSaved={refetchOrder} />}
          <OrderCustomerActionPanel
            status={order.status}
            pending={cancelMutation.isPending || delayMutation.isPending}
            error={cancelMutation.error ?? delayMutation.error}
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
      <GuestMemberGuide source="guest_order_lookup" />
    </Container>
  );
}
