import { useMutation } from "@tanstack/react-query";
import { Container } from "react-bootstrap";
import { cancelGuestOrder, fetchOrder, respondToGuestOrderDelay } from "@/features/order/api";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { GuestMemberGuide } from "@/features/guest-lookup/GuestMemberGuide";
import { GuestRecordDetailHeader } from "@/features/guest-lookup/GuestRecordDetailHeader";
import { useGuestRecordLookup } from "@/features/guest-lookup/useGuestRecordLookup";
import { OrderDetailCard } from "@/features/order/OrderDetailCard";
import { ShippingAddressEditPanel } from "@/features/order/ShippingAddressEditPanel";
import { OrderCustomerActionPanel } from "@/features/order/OrderCustomerActionPanel";
import { ErrorAlert } from "@/shared/ui";
import { OrderClaimSection } from "@/features/order-claim/OrderClaimSection";
import { runForCurrentCustomer } from "@/shared/api";

export function OrderDetailPage() {
  const { sessionVersion } = useCustomerAuth();
  return <OrderDetailContent key={sessionVersion} />;
}

function OrderDetailContent() {
  const orderLookup = useGuestRecordLookup("order", fetchOrder);
  const { lookup, query: { data: order, error, refetch: refetchOrder } } = orderLookup;
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

  return (
    <Container className="page-container guest-lookup-page">
      <GuestRecordDetailHeader
        lookup={orderLookup}
        found={Boolean(order)}
        description="조회 코드는 다른 사람과 공유하지 마세요."
        submitLabel="조회"
      />

      <ErrorAlert error={order ? null : error} />

      <ErrorAlert error={cancelMutation.error ?? delayMutation.error} />
      {order && (
        <>
          <OrderDetailCard order={order} />
          {lookup && <ShippingAddressEditPanel key={lookup.requestId} order={order} accessToken={lookup.credentials.token} onSaved={refetchOrder} />}
          <OrderCustomerActionPanel
            status={order.status}
            expectedShipDate={order.fulfillment?.expectedShipDate}
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
      <GuestMemberGuide />
    </Container>
  );
}
