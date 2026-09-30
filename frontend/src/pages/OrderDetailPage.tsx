import { useState } from "react";
import { skipToken, useMutation, useQuery } from "@tanstack/react-query";
import { Container } from "react-bootstrap";
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

  function handleLookup(id: number, token: string) {
    if (lookup?.credentials.id === id && lookup.credentials.token === token) {
      void refetchOrder();
      return;
    }
    setLookup({ credentials: { id, token }, requestId: crypto.randomUUID() });
  }

  return (
    <Container className="page-container guest-lookup-page">
      <GuestLookupPanel title="비회원 주문 조회" kind="orders">
        <GuestLookupForm kind="orders" onLookup={handleLookup} isLoading={isFetching}
          initialId={initialCredentials.orderId ? String(initialCredentials.orderId) : undefined}
          initialToken={initialCredentials.token} submitLabel="조회" />
      </GuestLookupPanel>

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
