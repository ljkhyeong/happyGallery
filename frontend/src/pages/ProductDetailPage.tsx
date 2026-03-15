import { useState } from "react";
import { useParams, Link, useNavigate } from "react-router-dom";
import { useQuery, useMutation } from "@tanstack/react-query";
import { Container, Card, Badge, Button, Form, Row, Col } from "react-bootstrap";
import { fetchProduct } from "@/features/product/api";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { api } from "@/shared/api";
import { LoadingSpinner, ErrorAlert, useToast } from "@/shared/ui";
import { formatKRW } from "@/shared/lib";

const TYPE_LABEL: Record<string, string> = {
  READY_STOCK: "기존 재고",
  MADE_TO_ORDER: "예약 제작",
};

const FULFILLMENT_LABEL: Record<string, string> = {
  READY_STOCK: "배송 상품 - 승인 후 출고됩니다.",
  MADE_TO_ORDER: "예약 제작 - 승인 후 제작이 시작됩니다.",
};

const MAX_QTY = 99;

interface MemberOrderResponse {
  orderId: number;
  status: string;
  totalAmount: number;
  paidAt: string;
  createdAt: string;
}

export function ProductDetailPage() {
  const { id } = useParams<{ id: string }>();
  const productId = Number(id);
  const navigate = useNavigate();
  const toast = useToast();
  const { isAuthenticated, isLoading: authLoading } = useCustomerAuth();

  const [qty, setQty] = useState(1);

  const { data: product, isLoading, error } = useQuery({
    queryKey: ["products", productId],
    queryFn: () => fetchProduct(productId),
    enabled: productId > 0,
  });

  const orderMutation = useMutation({
    mutationFn: () =>
      api<MemberOrderResponse>("/me/orders", {
        method: "POST",
        body: { items: [{ productId, qty }] },
      }),
    onSuccess: (order) => {
      toast.show("주문이 완료되었습니다!");
      navigate(`/my/orders/${order.orderId}`);
    },
  });

  if (isLoading) return <Container className="page-container"><LoadingSpinner /></Container>;
  if (error) return <Container className="page-container"><ErrorAlert error={error} /></Container>;
  if (!product) return null;

  const totalAmount = product.price * qty;
  const canBuy = product.available && qty >= 1 && qty <= MAX_QTY;

  return (
    <Container className="page-container" style={{ maxWidth: 640 }}>
      <Link to="/products" className="text-decoration-none small d-block mb-3">
        &larr; 상품 목록
      </Link>

      {/* 상품 정보 */}
      <Card className="mb-3">
        <Card.Body>
          <div className="d-flex justify-content-between align-items-start mb-3">
            <h4 className="mb-0">{product.name}</h4>
            <Badge bg={product.available ? "success" : "secondary"} className="badge-status">
              {product.available ? "구매 가능" : "품절"}
            </Badge>
          </div>
          <p className="text-muted-soft mb-2">{TYPE_LABEL[product.type] ?? product.type}</p>
          <h5 className="mb-3">{formatKRW(product.price)}</h5>
          <p className="text-muted-soft small mb-0">
            {FULFILLMENT_LABEL[product.type] ?? ""}
          </p>
        </Card.Body>
      </Card>

      {/* 구매 패널 */}
      <Card className="purchase-panel">
        <Card.Body>
          <Row className="align-items-center g-3 mb-3">
            <Col xs={4}>
              <Form.Label htmlFor="product-qty" className="mb-0 small fw-semibold">수량</Form.Label>
            </Col>
            <Col xs={8}>
              <div className="d-flex align-items-center gap-2">
                <Button
                  variant="outline-secondary"
                  size="sm"
                  disabled={qty <= 1}
                  onClick={() => setQty((q) => Math.max(1, q - 1))}
                  aria-label="수량 감소"
                >
                  -
                </Button>
                <Form.Control
                  id="product-qty"
                  type="number"
                  min={1}
                  max={MAX_QTY}
                  value={qty}
                  onChange={(e) => {
                    const v = Number(e.target.value);
                    if (v >= 1 && v <= MAX_QTY) setQty(v);
                  }}
                  className="text-center"
                  style={{ width: 64 }}
                />
                <Button
                  variant="outline-secondary"
                  size="sm"
                  disabled={qty >= MAX_QTY}
                  onClick={() => setQty((q) => Math.min(MAX_QTY, q + 1))}
                  aria-label="수량 증가"
                >
                  +
                </Button>
              </div>
            </Col>
          </Row>

          <div className="d-flex justify-content-between align-items-center mb-3 pt-2 border-top">
            <span className="fw-semibold">총 금액</span>
            <span className="fs-5 fw-bold">{formatKRW(totalAmount)}</span>
          </div>

          <ErrorAlert error={orderMutation.error} />

          {!authLoading && isAuthenticated ? (
            <Button
              variant="primary"
              size="lg"
              className="w-100"
              disabled={!canBuy || orderMutation.isPending}
              onClick={() => orderMutation.mutate()}
            >
              {orderMutation.isPending ? "주문 처리 중..." : "구매하기"}
            </Button>
          ) : !authLoading ? (
            <div>
              <Button
                variant="primary"
                size="lg"
                className="w-100 mb-2"
                disabled={!canBuy}
                onClick={() => navigate("/login", { state: { from: `/products/${productId}` } })}
              >
                로그인하고 구매하기
              </Button>
              <Button
                as={Link as any}
                to="/orders/new"
                variant="outline-secondary"
                className="w-100"
              >
                비회원 주문하기
              </Button>
            </div>
          ) : null}
        </Card.Body>
      </Card>
    </Container>
  );
}
