import { LinkButton } from "@/shared/ui/LinkButton";
import { useEffect, useRef, useState } from "react";
import { useParams, Link, useNavigate } from "react-router-dom";
import { useQuery, useMutation } from "@tanstack/react-query";
import { Alert, Container, Card, Badge, Button, Form, Row, Col } from "react-bootstrap";
import { ShoppingBag } from "lucide-react";
import { fetchProduct } from "@/features/product/api";
import { buildAuthPageHref } from "@/features/customer-auth/navigation";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import {
  executePaymentFlow,
  type OrderPayload,
} from "@/features/payment";
import { PUBLIC_DATA_STALE_TIME } from "@/shared/api/staleTimes";
import { LoadingSpinner, ErrorAlert, useToast } from "@/shared/ui";
import {
  formatKRW,
  isPositiveSafeIntegerString,
  PRODUCT_FULFILLMENT_LABEL,
  PRODUCT_TYPE_LABEL,
} from "@/shared/lib";
import { ProductQnaSection } from "@/features/product-qna/ProductQnaSection";
import { useCart } from "@/features/cart/useCart";
import { CartQuantityError } from "@/features/cart/useGuestCart";
import { NotFoundPage } from "@/pages/NotFoundPage";
import {
  FulfillmentForm,
  fulfillmentPayload,
  isFulfillmentComplete,
  useFulfillmentSelection,
} from "@/features/order/FulfillmentForm";
import { OrderPriceSummary } from "@/features/order/OrderPriceSummary";
import { MadeToOrderConsent } from "@/features/order/MadeToOrderConsent";
import {
  isMadeToOrderConsentVersionMismatch,
  useMadeToOrderConsent,
} from "@/features/order/useMadeToOrderConsent";
import { MAX_PRODUCT_QUANTITY } from "@/shared/validation/productQuantity";

export function ProductDetailPage() {
  const { id } = useParams<{ id: string }>();
  const productId = Number(id);
  const validProductId = isPositiveSafeIntegerString(id);
  const navigate = useNavigate();
  const toast = useToast();
  const { isAuthenticated, isLoading: authLoading, user } = useCustomerAuth();

  const [qty, setQty] = useState(1);
  const [showMobilePurchaseCta, setShowMobilePurchaseCta] = useState(false);
  const purchasePanelRef = useRef<HTMLDivElement>(null);
  const [fulfillment, setFulfillment] = useFulfillmentSelection(user?.name, user?.phone ?? undefined);
  const { addItem: addToCart } = useCart();

  const { data: product, isLoading, error } = useQuery({
    queryKey: ["products", productId],
    queryFn: () => fetchProduct(productId),
    enabled: validProductId,
    staleTime: PUBLIC_DATA_STALE_TIME,
  });
  const requiresMadeToOrderConsent = product?.type === "MADE_TO_ORDER";
  const consent = useMadeToOrderConsent(requiresMadeToOrderConsent);

  const orderMutation = useMutation({
    mutationFn: async () => {
      if (!user) throw new Error("로그인이 필요합니다.");
      const payload: OrderPayload = {
        type: "ORDER",
        userId: user.id,
        name: user.name,
        items: [{ productId, qty }],
        cartCheckout: false,
        madeToOrderConsent: consent.agreed,
        madeToOrderConsentVersion: consent.version,
        ...fulfillmentPayload(fulfillment),
      };
      await executePaymentFlow({
        context: "ORDER",
        payload,
        orderName: product ? `${product.name} (${qty}개)` : `상품 주문 (${qty}개)`,
        customerKey: `member_${user.id}`,
        customerName: user.name,
        customerPhone: user.phone || undefined,
        returnHint: { customerName: user.name, customerPhone: user.phone ?? undefined },
      });
    },
    onError: consent.handleSubmissionError,
  });
  const cartMutation = useMutation({
    mutationFn: () => addToCart(productId, qty),
    onSuccess: () => toast.show("장바구니에 추가되었습니다."),
  });
  const consentVersionMismatch = isMadeToOrderConsentVersionMismatch(orderMutation.error);

  useEffect(() => {
    const purchasePanel = purchasePanelRef.current;
    if (!purchasePanel || !product) return;

    const observer = new IntersectionObserver(([entry]) => {
      if (!entry) return;
      setShowMobilePurchaseCta(!entry.isIntersecting && entry.boundingClientRect.top > 0);
    }, { rootMargin: "0px 0px -30% 0px" });
    observer.observe(purchasePanel);
    return () => observer.disconnect();
  }, [product]);

  if (!validProductId) return <NotFoundPage />;
  if (isLoading) return <Container className="page-container"><LoadingSpinner /></Container>;
  if (error) return <Container className="page-container"><ErrorAlert error={error} /></Container>;
  if (!product) return null;

  const itemAmount = product.price * qty;
  const canBuy = product.available && qty >= 1 && qty <= MAX_PRODUCT_QUANTITY;
  const canCheckout = canBuy && isFulfillmentComplete(fulfillment);
  const guestFallbackPath = `/orders/new?productId=${productId}&qty=${qty}`;
  const memberRedirectPath = `/products/${productId}`;
  const loginHref = buildAuthPageHref("/login", { redirectTo: memberRedirectPath });
  const signupHref = buildAuthPageHref("/signup", { redirectTo: memberRedirectPath });

  return (
    <Container className="page-container">
      <div className="store-detail-breadcrumb anim-fade-up">
        <Link to="/" className="store-detail-breadcrumb-link">홈</Link>
        <span>/</span>
        <Link to="/products" className="store-detail-breadcrumb-link">작품</Link>
        <span>/</span>
        <span className="store-detail-breadcrumb-current">{product.name}</span>
      </div>

      <Row className="gx-0 gy-4 gx-lg-5 align-items-start">
        <Col lg={7} className="anim-fade-up anim-delay-1">
          <Card className="store-detail-card border-0">
            <Card.Body className="p-0">
              {product.imageUrl && (
                <div className="store-detail-media mb-4">
                  <img src={product.imageUrl} alt={product.name} />
                </div>
              )}
              <div className="d-flex flex-wrap justify-content-between align-items-start gap-3 mb-4">
                <div>
                  <div className="store-detail-kicker mb-2">{PRODUCT_TYPE_LABEL[product.type] ?? product.type}</div>
                  <h2 className="store-detail-title mb-2">{product.name}</h2>
                  <p className="text-muted-soft store-section-desc mb-0">
                    {product.description || (product.type === "MADE_TO_ORDER"
                      ? "주문 승인 후 제작을 시작하는 공방 제작 상품입니다."
                      : "재고 수량 기준으로 바로 주문을 접수하는 판매 상품입니다.")}
                  </p>
                </div>
                <Badge bg={product.available ? "dark" : "secondary"} className="badge-status py-2 px-3">
                  {product.available ? "구매 가능" : "품절"}
                </Badge>
              </div>

              <div className="store-detail-price mb-4">{formatKRW(product.price)}</div>

              <div className="store-detail-note-grid mb-4">
                <div className="store-detail-note">
                  <div className="store-detail-note-label">주문</div>
                  <div className="store-detail-note-body">수량과 수령 방법을 선택한 뒤 주문합니다</div>
                </div>
                <div className="store-detail-note">
                  <div className="store-detail-note-label">회원</div>
                  <div className="store-detail-note-body">결제 후 내 주문에서 바로 확인합니다</div>
                </div>
                <div className="store-detail-note">
                  <div className="store-detail-note-label">비회원</div>
                  <div className="store-detail-note-body">휴대폰 인증 후 주문하고 조회합니다</div>
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

        <Col lg={5} className="anim-fade-up anim-delay-2">
          <Card ref={purchasePanelRef} className="purchase-panel store-purchase-card">
            <Card.Body className="p-4">
              <div className="store-purchase-kicker mb-1">작품 주문</div>
              <h5 className="store-purchase-title mb-4">바로 주문하기</h5>

              <Row className="align-items-center g-3 mb-3">
                <Col xs={4}>
                  <Form.Label htmlFor="product-qty" className="mb-0 small store-purchase-qty-label">수량</Form.Label>
                </Col>
                <Col xs={8}>
                  <div className="d-flex align-items-center gap-2">
                    <Button
                      variant="outline-dark"
                      size="sm"
                      disabled={qty <= 1}
                      onClick={() => setQty((q) => Math.max(1, q - 1))}
                      aria-label="수량 감소"
                      className="store-purchase-qty-btn"
                    >
                      −
                    </Button>
                    <Form.Control
                      id="product-qty"
                      type="number"
                      min={1}
                      max={MAX_PRODUCT_QUANTITY}
                      value={qty}
                      onChange={(e) => {
                        const v = Number(e.target.value);
                        if (Number.isInteger(v) && v >= 1 && v <= MAX_PRODUCT_QUANTITY) setQty(v);
                      }}
                      className="text-center store-purchase-qty-input"
                    />
                    <Button
                      variant="outline-dark"
                      size="sm"
                      disabled={qty >= MAX_PRODUCT_QUANTITY}
                      onClick={() => setQty((q) => Math.min(MAX_PRODUCT_QUANTITY, q + 1))}
                      aria-label="수량 증가"
                      className="store-purchase-qty-btn"
                    >
                      +
                    </Button>
                  </div>
                </Col>
              </Row>

              <div className="store-purchase-summary mb-4">
                <div className="d-flex justify-content-between align-items-center mb-2">
                  <span className="text-muted-soft store-purchase-line">상품 금액</span>
                  <span className="store-purchase-line">{formatKRW(product.price)}</span>
                </div>
                <div className="d-flex justify-content-between align-items-center mb-2">
                  <span className="text-muted-soft store-purchase-line">선택 수량</span>
                  <span className="store-purchase-line">{qty}개</span>
                </div>
                <OrderPriceSummary
                  itemAmount={itemAmount}
                  fulfillmentType={fulfillment.fulfillmentType}
                  className="pt-2"
                />
              </div>

              {!authLoading && isAuthenticated && (
                <div className="mb-4">
                  <FulfillmentForm value={fulfillment} onChange={setFulfillment} />
                </div>
              )}

              <ErrorAlert error={consentVersionMismatch ? null : orderMutation.error} />
              {cartMutation.error instanceof CartQuantityError ? (
                <Alert variant="danger">{cartMutation.error.message}</Alert>
              ) : (
                <ErrorAlert error={cartMutation.error} />
              )}

              {!authLoading && isAuthenticated ? (
                <>
                  <MadeToOrderConsent
                    required={requiresMadeToOrderConsent}
                    policy={consent.policyQuery.data}
                    isLoading={consent.policyQuery.isLoading}
                    isFetching={consent.policyQuery.isFetching}
                    error={consent.policyQuery.error}
                    checked={consent.checked}
                    onChange={consent.setChecked}
                    versionMismatch={consent.versionMismatch}
                    refreshRequired={consent.refreshRequired}
                  />
                  <Button
                    variant="dark"
                    size="lg"
                    className="w-100 mb-2 store-purchase-btn-primary"
                    disabled={!canCheckout || !consent.ready || orderMutation.isPending}
                    onClick={() => orderMutation.mutate()}
                  >
                    {orderMutation.isPending ? "주문 처리 중..." : "바로 구매하기"}
                  </Button>
                  <Button
                    variant="outline-dark"
                    size="lg"
                    className="w-100 mb-2"
                    disabled={!canBuy || cartMutation.isPending}
                    onClick={() => cartMutation.mutate()}
                  >
                    {cartMutation.isPending ? "담는 중..." : "장바구니 담기"}
                  </Button>
                  <p className="store-purchase-helper mb-0">
                    결제가 완료되면 바로 내 주문 상세로 이동합니다.
                  </p>
                </>
              ) : !authLoading ? (
                <>
                  <Button
                    variant="dark"
                    size="lg"
                    className="w-100 mb-2 store-purchase-btn-primary"
                    disabled={!canBuy}
                    onClick={() => navigate(loginHref)}
                  >
                    로그인 후 구매하기
                  </Button>
                  <LinkButton
                    to={signupHref}
                    variant="outline-dark"
                    className="w-100 mb-2"
                  >
                    회원가입 후 구매하기
                  </LinkButton>
                  <Button
                    variant="outline-dark"
                    className="w-100 mb-2"
                    disabled={!canBuy || cartMutation.isPending}
                    onClick={() => cartMutation.mutate()}
                  >
                    {cartMutation.isPending ? "담는 중..." : "장바구니 담기"}
                  </Button>
                  <LinkButton
                    to={guestFallbackPath}
                    variant="link"
                    className="w-100 text-muted-soft store-purchase-guest-link"
                  >
                    비회원 주문하기 →
                  </LinkButton>
                  <p className="store-purchase-helper mb-0 mt-2">
                    비회원 주문은 별도 경로로 이어지며, 선택한 상품과 수량을 미리 담아둡니다.
                  </p>
                </>
              ) : null}
            </Card.Body>
          </Card>
        </Col>
      </Row>

      {showMobilePurchaseCta && product.available && (
        <div className="store-mobile-purchase-cta d-lg-none">
          <Button
            variant="dark"
            size="sm"
            className="w-100 d-flex align-items-center justify-content-center gap-2"
            onClick={() => purchasePanelRef.current?.scrollIntoView({
              behavior: "smooth",
              block: "start",
            })}
          >
            <ShoppingBag size={17} aria-hidden="true" />
            구매 옵션 보기
          </Button>
        </div>
      )}

      <ProductQnaSection productId={productId} />
    </Container>
  );
}
