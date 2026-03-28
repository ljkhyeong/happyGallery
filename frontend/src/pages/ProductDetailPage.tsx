import { useState } from "react";
import { useParams, Link, useNavigate } from "react-router-dom";
import { useQuery, useMutation } from "@tanstack/react-query";
import { Container, Card, Badge, Button, Form, Row, Col } from "react-bootstrap";
import { fetchProduct } from "@/features/product/api";
import { buildAuthPageHref } from "@/features/customer-auth/navigation";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { api } from "@/shared/api";
import { PUBLIC_DATA_STALE_TIME } from "@/shared/api/staleTimes";
import { LoadingSpinner, ErrorAlert, useToast } from "@/shared/ui";
import { formatKRW, PRODUCT_TYPE_LABEL, PRODUCT_FULFILLMENT_LABEL } from "@/shared/lib";
import { ProductQnaSection } from "@/features/product-qna/ProductQnaSection";
import { useCart } from "@/features/cart/useCart";

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
  const { addItem: addToCart } = useCart();

  const { data: product, isLoading, error } = useQuery({
    queryKey: ["products", productId],
    queryFn: () => fetchProduct(productId),
    enabled: productId > 0,
    staleTime: PUBLIC_DATA_STALE_TIME,
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
  const guestFallbackPath = `/orders/new?productId=${productId}&qty=${qty}`;
  const memberRedirectPath = `/products/${productId}`;
  const loginHref = buildAuthPageHref("/login", { redirectTo: memberRedirectPath });
  const signupHref = buildAuthPageHref("/signup", { redirectTo: memberRedirectPath });

  return (
    <Container className="page-container">
      <div className="store-detail-breadcrumb">
        <Link to="/" className="text-decoration-none">홈</Link>
        <span>/</span>
        <Link to="/products" className="text-decoration-none">상품</Link>
        <span>/</span>
        <span>{product.name}</span>
      </div>

      <Row className="g-4 align-items-start">
        <Col lg={7}>
          <Card className="store-detail-card">
            <Card.Body>
              <div className="d-flex flex-wrap justify-content-between align-items-start gap-3 mb-4">
                <div>
                  <div className="store-detail-kicker">{PRODUCT_TYPE_LABEL[product.type] ?? product.type}</div>
                  <h2 className="store-detail-title mb-2">{product.name}</h2>
                  <p className="text-muted-soft mb-0">
                    {product.type === "MADE_TO_ORDER"
                      ? "주문 승인 후 제작을 시작하는 공방 제작 상품입니다."
                      : "재고 수량 기준으로 바로 주문을 접수하는 판매 상품입니다."}
                  </p>
                </div>
                <Badge bg={product.available ? "success" : "secondary"} className="badge-status">
                  {product.available ? "구매 가능" : "품절"}
                </Badge>
              </div>

              <div className="store-detail-price mb-4">{formatKRW(product.price)}</div>

              <div className="store-detail-note-grid mb-4">
                <div className="store-detail-note">
                  <div className="store-detail-note-label">구매 흐름</div>
                  <div className="store-detail-note-body">상품 상세에서 수량 선택 후 바로 주문</div>
                </div>
                <div className="store-detail-note">
                  <div className="store-detail-note-label">회원 주문</div>
                  <div className="store-detail-note-body">결제 후 `/my/orders/:id` 에서 즉시 확인</div>
                </div>
                <div className="store-detail-note">
                  <div className="store-detail-note-label">비회원 주문</div>
                  <div className="store-detail-note-body">legacy 주문 fallback 경로로 계속 지원</div>
                </div>
              </div>

              <Card className="store-detail-info-card">
                <Card.Body>
                  <div className="store-detail-info-title">이행 안내</div>
                  <p className="mb-0 text-muted-soft small">
                    {PRODUCT_FULFILLMENT_LABEL[product.type] ?? ""}
                  </p>
                </Card.Body>
              </Card>
            </Card.Body>
          </Card>
        </Col>

        <Col lg={5}>
          <Card className="purchase-panel store-purchase-card">
            <Card.Body>
              <div className="store-purchase-kicker">Quick Order</div>
              <h5 className="mb-3">이 상품으로 바로 주문하기</h5>

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
                      style={{ width: 72 }}
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

              <div className="store-purchase-summary mb-3">
                <div className="d-flex justify-content-between align-items-center">
                  <span className="text-muted-soft">상품 금액</span>
                  <span>{formatKRW(product.price)}</span>
                </div>
                <div className="d-flex justify-content-between align-items-center">
                  <span className="text-muted-soft">선택 수량</span>
                  <span>{qty}개</span>
                </div>
                <div className="d-flex justify-content-between align-items-center pt-2 mt-2 border-top">
                  <span className="fw-semibold">총 금액</span>
                  <span className="fs-5 fw-bold">{formatKRW(totalAmount)}</span>
                </div>
              </div>

              <ErrorAlert error={orderMutation.error} />

              {!authLoading && isAuthenticated ? (
                <>
                  <Button
                    variant="primary"
                    size="lg"
                    className="w-100 mb-2"
                    disabled={!canBuy || orderMutation.isPending}
                    onClick={() => orderMutation.mutate()}
                  >
                    {orderMutation.isPending ? "주문 처리 중..." : "구매하기"}
                  </Button>
                  <Button
                    variant="outline-primary"
                    size="lg"
                    className="w-100 mb-2"
                    disabled={!canBuy}
                    onClick={() => {
                      addToCart(productId, qty);
                      toast.show("장바구니에 추가되었습니다.");
                    }}
                  >
                    장바구니 담기
                  </Button>
                  <p className="store-purchase-helper mb-0">
                    주문이 완료되면 바로 내 주문 상세로 이동합니다.
                  </p>
                </>
              ) : !authLoading ? (
                <>
                  <Button
                    variant="primary"
                    size="lg"
                    className="w-100 mb-2"
                    disabled={!canBuy}
                    onClick={() => navigate(loginHref)}
                  >
                    로그인하고 구매하기
                  </Button>
                  <Button
                    as={Link as any}
                    to={signupHref}
                    variant="outline-dark"
                    className="w-100 mb-2"
                  >
                    회원가입 후 구매하기
                  </Button>
                  <Button
                    variant="outline-primary"
                    className="w-100 mb-2"
                    disabled={!canBuy}
                    onClick={() => {
                      addToCart(productId, qty);
                      toast.show("장바구니에 추가되었습니다.");
                    }}
                  >
                    장바구니 담기
                  </Button>
                  <Button
                    as={Link as any}
                    to={guestFallbackPath}
                    variant="outline-secondary"
                    className="w-100"
                  >
                    비회원 주문하기
                  </Button>
                  <p className="store-purchase-helper mb-0 mt-3">
                    비회원 주문은 fallback 경로로 이어지며, 현재 선택한 상품과 수량을 미리 담아둡니다.
                  </p>
                </>
              ) : null}
            </Card.Body>
          </Card>
        </Col>
      </Row>

      <ProductQnaSection productId={productId} />
    </Container>
  );
}
