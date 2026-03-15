import { useParams, Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { Button, Container } from "react-bootstrap";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { MyAuthGateCard } from "@/features/my/MyAuthGateCard";
import { api } from "@/shared/api";
import { OrderDetailCard } from "@/features/order/OrderDetailCard";
import { LoadingSpinner, ErrorAlert } from "@/shared/ui";
import type { OrderDetailResponse } from "@/shared/types";

export function MyOrderDetailPage() {
  const { id } = useParams<{ id: string }>();
  const orderId = Number(id);
  const { isAuthenticated, isLoading: authLoading } = useCustomerAuth();

  const { data: order, isLoading, error } = useQuery({
    queryKey: ["my", "orders", orderId],
    queryFn: () => api<OrderDetailResponse>(`/me/orders/${orderId}`),
    enabled: isAuthenticated && orderId > 0,
  });

  if (authLoading || isLoading) {
    return <Container className="page-container"><LoadingSpinner /></Container>;
  }

  if (error) {
    return <Container className="page-container"><ErrorAlert error={error} /></Container>;
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

  if (!order) return null;

  return (
    <Container className="page-container" style={{ maxWidth: 640 }}>
      <div className="my-detail-header">
        <div className="d-flex flex-wrap justify-content-between gap-2 align-items-start mb-3">
          <Link to="/my/orders" className="text-decoration-none small">
            &larr; 내 주문
          </Link>
          <Button as={Link as any} to="/products" variant="outline-secondary" size="sm">
            스토어 둘러보기
          </Button>
        </div>
        <div className="my-section-kicker mb-2">My Order</div>
        <h4 className="mb-2">주문 상세</h4>
        <p className="text-muted-soft small mb-0">
          현재 주문 상태와 이행 정보를 확인할 수 있습니다.
        </p>
      </div>
      <OrderDetailCard order={order} />
    </Container>
  );
}
