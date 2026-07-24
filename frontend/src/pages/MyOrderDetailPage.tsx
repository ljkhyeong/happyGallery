import { LinkButton } from "@/shared/ui/LinkButton";
import { useParams, Link } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Container } from "react-bootstrap";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { MyAuthGateCard } from "@/features/my/MyAuthGateCard";
import { api, queryKeys } from "@/shared/api";
import { OrderDetailCard } from "@/features/order/OrderDetailCard";
import { OrderCustomerActionPanel } from "@/features/order/OrderCustomerActionPanel";
import { cancelMyOrder, respondToMyOrderDelay } from "@/features/order/api";
import { LoadingSpinner, ErrorAlert } from "@/shared/ui";
import type { OrderDetailResponse } from "@/shared/types";
import { customerRefundPollingInterval, isPositiveSafeIntegerString } from "@/shared/lib";
import { NotFoundPage } from "@/pages/NotFoundPage";
import { OrderClaimSection } from "@/features/order-claim/OrderClaimSection";

export function MyOrderDetailPage() {
  const { id } = useParams<{ id: string }>();
  const orderId = Number(id);
  const validOrderId = isPositiveSafeIntegerString(id);
  const { isAuthenticated, isLoading: authLoading } = useCustomerAuth();
  const queryClient = useQueryClient();

  const { data: order, isLoading, error } = useQuery({
    queryKey: queryKeys.member.orders.detail(orderId),
    queryFn: () => api<OrderDetailResponse>(`/me/orders/${orderId}`),
    enabled: isAuthenticated && validOrderId,
    refetchInterval: ({ state }) =>
      customerRefundPollingInterval(
        state.data?.refund?.status,
        state.dataUpdateCount + state.fetchFailureCount,
      ),
  });
  const cancelMutation = useMutation({
    mutationFn: () => cancelMyOrder(orderId),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: queryKeys.member.orders.all }),
  });
  const delayMutation = useMutation({
    mutationFn: (decision: "ACCEPT" | "REJECT") =>
      respondToMyOrderDelay(orderId, decision),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: queryKeys.member.orders.all }),
  });

  if (!validOrderId) return <NotFoundPage />;

  if (authLoading || isLoading) {
    return <Container className="page-container"><LoadingSpinner /></Container>;
  }

  if (!isAuthenticated) {
    return (
      <Container className="page-container" style={{ maxWidth: 640 }}>
        <MyAuthGateCard
          title="로그인이 필요합니다"
          description="회원 주문 상세는 로그인 후 내 정보에서 바로 확인할 수 있습니다."
        />
      </Container>
    );
  }

  if (error && !order) {
    return <Container className="page-container"><ErrorAlert error={error} /></Container>;
  }

  if (!order) return null;

  return (
    <Container className="page-container" style={{ maxWidth: 640 }}>
      <div className="my-detail-header">
        <div className="d-flex flex-wrap justify-content-between gap-2 align-items-start mb-3">
          <Link to="/my/orders" className="text-decoration-none small">
            &larr; 내 주문
          </Link>
          <LinkButton to="/products" variant="outline-secondary" size="sm">
            스토어 둘러보기
          </LinkButton>
        </div>
        <div className="my-section-kicker mb-2">My Order</div>
        <h4 className="mb-2">주문 상세</h4>
        <p className="text-muted-soft small mb-0">
          현재 주문 상태와 이행 정보를 확인할 수 있습니다.
        </p>
      </div>
      {error && <ErrorAlert error={error} />}
      <ErrorAlert error={cancelMutation.error ?? delayMutation.error} />
      <OrderDetailCard order={order} />
      <OrderCustomerActionPanel
        status={order.status}
        pending={cancelMutation.isPending || delayMutation.isPending}
        onCancel={() => cancelMutation.mutate()}
        onDelayDecision={(decision) => delayMutation.mutate(decision)}
      />
      <OrderClaimSection order={order} access={{ kind: "member" }} />
    </Container>
  );
}
