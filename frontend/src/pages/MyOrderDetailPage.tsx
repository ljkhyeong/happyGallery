import { LinkButton } from "@/shared/ui/LinkButton";
import { useParams, Link, useLocation } from "react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { myNavLabel } from "@/features/my/myNavigation";
import { queryKeys, runForCurrentCustomer } from "@/shared/api";
import { getMyOrder } from "@/generated/api/customerStore";
import { OrderDetailCard } from "@/features/order/OrderDetailCard";
import { ShippingAddressEditPanel } from "@/features/order/ShippingAddressEditPanel";
import { OrderCustomerActionPanel } from "@/features/order/OrderCustomerActionPanel";
import { cancelMyOrder, respondToMyOrderDelay } from "@/features/order/api";
import { LoadingSpinner, ErrorAlert, PageHeader } from "@/shared/ui";
import { customerRefundPollingInterval, isPositiveSafeIntegerString } from "@/shared/lib";
import { NotFoundPage } from "@/pages/NotFoundPage";
import { OrderClaimSection } from "@/features/order-claim/OrderClaimSection";
import { OrderReviewsSection } from "@/features/review/OrderReviewsSection";
import { PaymentReceiptLink } from "@/features/payment/PaymentReceiptLink";

export function MyOrderDetailPage() {
  const { search } = useLocation();
  const { id } = useParams<{ id: string }>();
  const orderId = Number(id);
  const validOrderId = isPositiveSafeIntegerString(id);
  const { isAuthenticated } = useCustomerAuth();
  const queryClient = useQueryClient();

  const { data: order, isLoading, isFetching, error, refetch } = useQuery({
    queryKey: queryKeys.member.orders.detail(orderId),
    queryFn: () => getMyOrder(orderId),
    enabled: isAuthenticated && validOrderId,
    refetchInterval: ({ state }) =>
      customerRefundPollingInterval(
        state.data?.refund?.status,
        state.dataUpdateCount + state.fetchFailureCount,
      ),
  });
  const cancelMutation = useMutation({
    mutationFn: () => runForCurrentCustomer(
      () => cancelMyOrder(orderId),
      () => queryClient.invalidateQueries({ queryKey: queryKeys.member.orders.all }),
    ),
  });
  const delayMutation = useMutation({
    mutationFn: (decision: "ACCEPT" | "REJECT") =>
      runForCurrentCustomer(
        () => respondToMyOrderDelay(orderId, decision),
        () => queryClient.invalidateQueries({ queryKey: queryKeys.member.orders.all }),
      ),
  });

  if (!validOrderId) return <NotFoundPage />;

  return (
    <>
      <Link to={{ pathname: "/my/orders", search }} className="my-back-link">
        &larr; {myNavLabel("/my/orders")}
      </Link>
      <PageHeader
        title="주문 상세"
        description="주문 상태와 배송·수령 정보를 확인하세요."
        actions={<LinkButton to="/products" variant="outline-dark" size="sm">작품 보러가기</LinkButton>}
      />
      <ErrorAlert error={error} onRetry={() => void refetch()} retrying={isFetching} />
      {isLoading && <LoadingSpinner />}
      {order && (
        <>
          <ErrorAlert error={cancelMutation.error ?? delayMutation.error} />
          <OrderDetailCard order={order} />
          <ShippingAddressEditPanel order={order} onSaved={() => queryClient.invalidateQueries({ queryKey: queryKeys.member.orders.all })} />
          {order.receiptUrl && <div className="mb-3"><PaymentReceiptLink receiptUrl={order.receiptUrl} /></div>}
          <OrderCustomerActionPanel
            status={order.status}
            pending={cancelMutation.isPending || delayMutation.isPending}
            error={cancelMutation.error ?? delayMutation.error}
            onCancel={() => cancelMutation.mutate()}
            onDelayDecision={(decision) => delayMutation.mutate(decision)}
          />
          <OrderReviewsSection orderId={order.orderId} items={order.items} />
          <OrderClaimSection order={order} access={{ kind: "member" }} />
        </>
      )}
    </>
  );
}
