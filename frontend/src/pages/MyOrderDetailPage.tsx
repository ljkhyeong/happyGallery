import { useParams, Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { Container } from "react-bootstrap";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
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

  if (!order) return null;

  return (
    <Container className="page-container" style={{ maxWidth: 640 }}>
      <Link to="/my" className="text-decoration-none small d-block mb-3">
        &larr; 내 정보
      </Link>
      <OrderDetailCard order={order} />
    </Container>
  );
}
