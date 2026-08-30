import { LinkButton } from "@/shared/ui/LinkButton";
import { useEffect, useMemo, useRef, useState } from "react";
import { Link, useNavigate } from "react-router";
import { useMutation } from "@tanstack/react-query";
import { Alert, Container, Card, Button, Form, Row, Col } from "react-bootstrap";
import { ShoppingBag } from "lucide-react";
import { fetchProduct } from "@/features/product/api";
import { buildAuthPageHref } from "@/features/customer-auth/navigation";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import {
  executePaymentFlow,
  PaymentMethodFields,
  PaymentErrorAlert,
  useCheckoutSelection,
  type OrderPayload,
} from "@/features/payment";
import { PUBLIC_DATA_STALE_TIME } from "@/shared/api/staleTimes";
import { LoadingSpinner, ErrorAlert, useToast } from "@/shared/ui";
import {
  formatKRW,
  PRODUCT_FULFILLMENT_LABEL,
  PRODUCT_TYPE_LABEL,
} from "@/shared/lib";
import { ProductQnaSection } from "@/features/product-qna/ProductQnaSection";
import { useCart } from "@/features/cart/useCart";
import { CartQuantityError } from "@/features/cart/useGuestCart";
import {
  FulfillmentForm,
  fulfillmentPayload,
  isFulfillmentComplete,
  useFulfillmentSelection,
} from "@/features/order/FulfillmentForm";
import { OrderPriceSummary } from "@/features/order/OrderPriceSummary";
import { MemberOrderBenefits } from "@/features/order-benefit/MemberOrderBenefits";
import { MadeToOrderConsent } from "@/features/order/MadeToOrderConsent";
import {
  isMadeToOrderConsentVersionMismatch,
  useMadeToOrderConsent,
} from "@/features/order/useMadeToOrderConsent";
import { queryKeys, runForCurrentCustomer, useLoaderBackedQuery } from "@/shared/api";
import { MAX_PRODUCT_QUANTITY } from "@/shared/validation/productQuantity";
import { ProductPurchaseTerms } from "@/features/product/ProductPurchaseTerms";
import { sumQuantitiesByVariant } from "@/features/product/purchaseQuantity";
import { productSelectionView } from "@/features/product/productSelectionView";
import { PublicReviewSection } from "@/features/review/PublicReviewSection";
import type { ProductDetailResponse } from "@/generated/api/product";
import {
  ProductPurchaseOptions,
  type PurchaseLine,
} from "@/features/product/ProductPurchaseOptions";

export function ProductDetailPage({ initialProduct }: { initialProduct: ProductDetailResponse }) {
  const { sessionVersion } = useCustomerAuth();
  return <ProductDetailContent key={sessionVersion} initialProduct={initialProduct} />;
}

function ProductDetailContent({ initialProduct }: { initialProduct: ProductDetailResponse }) {
  const productId = initialProduct.id;
  const productQueryKey = useMemo(
    () => queryKeys.catalog.productDetail(productId),
    [productId],
  );
  const navigate = useNavigate();
  const toast = useToast();
  const { isAuthenticated, isLoading: authLoading, user } = useCustomerAuth();

  const [qty, setQty] = useState(1);
  const [checkoutSelection, setCheckoutSelection] = useCheckoutSelection();
  const [purchaseLines, setPurchaseLines] = useState<PurchaseLine[]>([]);
  const [issuedCouponId, setIssuedCouponId] = useState<number | null>(null);
  const [rewardAmount, setRewardAmount] = useState(0);
  const [showMobilePurchaseCta, setShowMobilePurchaseCta] = useState(false);
  const purchasePanelRef = useRef<HTMLDivElement>(null);
  const [fulfillment, setFulfillment] = useFulfillmentSelection(
    user?.name,
    user?.phone ?? undefined,
  );
  const { addItems: addToCart } = useCart();
  const cartRequestRef = useRef<{ fingerprint: string; idempotencyKey: string } | null>(null);

  const {
    data: product,
    error,
    isLoading,
  } = useLoaderBackedQuery({
    queryKey: productQueryKey,
    queryFn: () => fetchProduct(productId),
    staleTime: PUBLIC_DATA_STALE_TIME,
  }, initialProduct);
  const requiresMadeToOrderConsent = product?.type === "MADE_TO_ORDER";
  const consent = useMadeToOrderConsent(requiresMadeToOrderConsent);

  const orderMutation = useMutation({
    mutationFn: async () => {
      if (!user) throw new Error("로그인이 필요합니다.");
      const items = (product?.optionGroups.length || purchaseLines.length)
        ? purchaseLines.map((line) => ({
          productId,
          productVariantId: line.productVariantId,
          textInputs: line.textInputs,
          qty: line.qty,
        }))
        : [{
          productId,
          productVariantId: product?.type === "MADE_TO_ORDER"
            ? (product.variants[0]?.id ?? null)
            : null,
          textInputs: [],
          qty,
        }];
      const payload: OrderPayload = {
        type: "ORDER",
        userId: user.id,
        name: user.name,
        items,
        cartCheckout: false,
        ...(issuedCouponId === null ? {} : { issuedCouponId }),
        rewardAmount,
        madeToOrderConsent: consent.agreed,
        madeToOrderConsentVersion: consent.version,
        ...fulfillmentPayload(fulfillment),
      };
      await executePaymentFlow({
        checkoutSelection,
        context: "ORDER",
        payload,
        orderName: product
          ? `${product.name} (${items.reduce((sum, item) => sum + item.qty, 0)}개)`
          : "상품 주문",
        customerKey: `member_${user.id}`,
        customerName: user.name,
        customerPhone: user.phone || undefined,
        returnHint: {
          customerName: user.name, customerPhone: user.phone ?? undefined,
          returnPath: `/products/${productId}`,
        },
      });
    },
    onError: consent.handleSubmissionError,
  });
  const cartMutation = useMutation({
    mutationFn: () => runForCurrentCustomer(
      async () => {
        if (!product) throw new Error("상품 정보를 확인할 수 없습니다.");
        const items = (product.optionGroups.length || purchaseLines.length)
          ? purchaseLines.map((line) => ({
            productId, productVariantId: line.productVariantId, textInputs: line.textInputs, qty: line.qty,
          }))
          : [{ productId, productVariantId: product.type === "MADE_TO_ORDER"
            ? (product.variants[0]?.id ?? null) : null, textInputs: [], qty }];
        const fingerprint = JSON.stringify(items);
        if (cartRequestRef.current?.fingerprint !== fingerprint) {
          cartRequestRef.current = { fingerprint, idempotencyKey: crypto.randomUUID() };
        }
        await addToCart(items, cartRequestRef.current.idempotencyKey);
      },
      () => {
        cartRequestRef.current = null;
        toast.show("장바구니에 추가되었습니다.");
      },
    ),
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

  if (isLoading) return <Container className="page-container"><LoadingSpinner /></Container>;
  if (error) return <Container className="page-container"><ErrorAlert error={error} /></Container>;
  if (!product) return null;

  const hasConfiguredOptions = product.optionGroups.length > 0 || purchaseLines.length > 0;
  const selectionViews = purchaseLines.map((line) => productSelectionView(product, line));
  const selectionChanged = selectionViews.some((view, index) => {
    const initial = productSelectionView(initialProduct, purchaseLines[index]!);
    return view.unitPrice !== initial.unitPrice || view.label !== initial.label
      || view.configurationValid !== initial.configurationValid;
  });
  const selectedQuantity = hasConfiguredOptions
    ? purchaseLines.reduce((sum, line) => sum + line.qty, 0)
    : qty;
  const defaultSelection = productSelectionView(product, {
    productVariantId: product.type === "MADE_TO_ORDER" ? product.variants[0]?.id : null,
  });
  const itemAmount = hasConfiguredOptions
    ? purchaseLines.reduce((sum, line, index) => sum + selectionViews[index]!.unitPrice * line.qty, 0)
    : defaultSelection.unitPrice * qty;
  const selectedQuantities = sumQuantitiesByVariant(purchaseLines);
  const canBuy = product.available
    && selectedQuantity >= 1
    && (hasConfiguredOptions || selectedQuantity <= MAX_PRODUCT_QUANTITY)
    && (hasConfiguredOptions || defaultSelection.configurationValid)
    && selectionViews.every((view) => view.configurationValid)
    && [...selectedQuantities].every(([variantId, quantity]) => quantity <= Math.min(
      MAX_PRODUCT_QUANTITY,
      product.variants.find((variant) => variant.id === variantId)?.quantity ?? 0,
    ));
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
          <article className="store-detail-card">
            <header className="store-product-intro">
              <div className="store-detail-meta">
                <span className="store-detail-kicker">
                  {PRODUCT_TYPE_LABEL[product.type] ?? "상품 종류 확인 필요"}
                </span>
                {product.category && (
                  <span className="store-detail-category">{product.category}</span>
                )}
                <span className={`store-detail-availability ${product.available ? "is-available" : "is-sold-out"}`}>
                  {product.available ? "주문 가능" : "품절"}
                </span>
              </div>
              <h1 className="store-detail-title">{product.name}</h1>
              <p className="text-muted-soft store-section-desc store-detail-description">
                {product.description || (product.type === "MADE_TO_ORDER"
                  ? "주문 승인 후 제작을 시작하는 공방 제작 상품입니다."
                  : "재고 수량 기준으로 바로 주문을 접수하는 판매 상품입니다.")}
              </p>
              <div className="store-detail-price-block">
                <span>기본가</span>
                <strong className="store-detail-price">{formatKRW(product.price)}</strong>
              </div>
            </header>

            {product.imageUrl && (
              <div className="store-detail-media">
                <img src={product.imageUrl} alt={product.name} />
              </div>
            )}

            <section className="store-detail-facts" aria-labelledby="product-information-title">
              <div className="store-detail-section-heading">
                <h2 id="product-information-title">작품 안내</h2>
                <span>Piece information</span>
              </div>
              <div className="store-detail-terms">
                <ProductPurchaseTerms
                  productName={product.name}
                  type={product.type}
                  specification={product.specification}
                  careInstructions={product.careInstructions}
                  productionLeadDays={product.productionLeadDays}
                />
              </div>
              <div className="store-detail-fulfillment">
                <strong>배송·수령</strong>
                <span>{PRODUCT_FULFILLMENT_LABEL[product.type] ?? ""}</span>
              </div>
            </section>
          </article>
        </Col>

        <Col lg={5} className="anim-fade-up anim-delay-2">
          <Card ref={purchasePanelRef} className="purchase-panel store-purchase-card store-order-sheet">
            <Card.Body className="p-4 p-xl-5">
              <header className="store-order-sheet-header">
                <div>
                  <div className="store-purchase-kicker">Atelier order</div>
                  <h2>공방 주문표</h2>
                  <p>원하는 옵션을 고른 뒤 주문 방법을 선택해 주세요.</p>
                </div>
                <div className="store-order-sheet-base-price">
                  <span>기본가</span>
                  <strong>{formatKRW(product.price)}</strong>
                </div>
              </header>

              <section className="store-order-sheet-step" aria-labelledby="product-option-title">
                <div className="store-order-sheet-step-heading">
                  <span aria-hidden="true">1</span>
                  <h3 id="product-option-title">옵션 선택</h3>
                </div>
                {selectionChanged && (
                  <Alert variant="info" className="py-2">
                    상품 가격 또는 옵션 정보가 변경되었습니다. 현재 표시된 옵션과 금액을 확인해 주세요.
                  </Alert>
                )}
                {hasConfiguredOptions ? (
                  <ProductPurchaseOptions
                    product={product}
                    lines={purchaseLines}
                    onChange={setPurchaseLines}
                  />
                ) : (
                  <div className="store-purchase-quantity">
                    <Form.Label htmlFor="product-qty" className="store-purchase-qty-label">수량</Form.Label>
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
                  </div>
                )}
              </section>

              <div className="store-purchase-summary">
                <div className="d-flex justify-content-between align-items-center mb-2">
                  <span className="text-muted-soft store-purchase-line">선택 수량</span>
                  <span className="store-purchase-line">{selectedQuantity}개</span>
                </div>
                {!authLoading && isAuthenticated ? (
                  <MemberOrderBenefits
                    productAmount={itemAmount}
                    fulfillmentType={fulfillment.fulfillmentType}
                    selectedCouponId={issuedCouponId}
                    rewardPointsToUse={rewardAmount}
                    disabled={orderMutation.isPending}
                    onCouponChange={setIssuedCouponId}
                    onRewardPointsChange={setRewardAmount}
                  />
                ) : (
                  <OrderPriceSummary
                    itemAmount={itemAmount}
                    fulfillmentType={fulfillment.fulfillmentType}
                    className="pt-2"
                  />
                )}
              </div>

              {!authLoading && isAuthenticated && (
                <section className="store-order-sheet-step" aria-labelledby="product-order-title">
                  <div className="store-order-sheet-step-heading">
                    <span aria-hidden="true">2</span>
                    <h3 id="product-order-title">주문 정보</h3>
                  </div>
                  <FulfillmentForm value={fulfillment} onChange={setFulfillment} />
                </section>
              )}

              <div className="store-order-sheet-actions">
                <PaymentErrorAlert error={consentVersionMismatch ? null : orderMutation.error} />
                {cartMutation.error instanceof CartQuantityError ? (
                  <Alert variant="danger">{cartMutation.error.message}</Alert>
                ) : (
                  <ErrorAlert error={cartMutation.error} />
                )}

                {!authLoading && isAuthenticated ? (
                  <>
                    <PaymentMethodFields value={checkoutSelection} onChange={setCheckoutSelection} disabled={orderMutation.isPending} />
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
                      결제 완료 화면에서 내 주문 상세를 확인할 수 있습니다.
                    </p>
                  </>
                ) : !authLoading ? (
                  <section aria-labelledby="guest-order-title">
                    <div className="store-order-sheet-step-heading store-order-sheet-action-heading">
                      <span aria-hidden="true">2</span>
                      <h3 id="guest-order-title">주문 방법</h3>
                    </div>
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
                    {hasConfiguredOptions ? (
                      <Button
                        variant="link"
                        className="w-100 text-muted-soft store-purchase-guest-link"
                        disabled={!canBuy}
                        onClick={() => {
                          sessionStorage.setItem("hg_guest_order_draft", JSON.stringify({
                            productId,
                            items: purchaseLines,
                          }));
                          navigate(`/orders/new?productId=${productId}&draft=options`);
                        }}
                      >
                        비회원 주문하기 →
                      </Button>
                    ) : (
                      <LinkButton
                        to={guestFallbackPath}
                        variant="link"
                        className="w-100 text-muted-soft store-purchase-guest-link"
                      >
                        비회원 주문하기 →
                      </LinkButton>
                    )}
                    <p className="store-purchase-helper mb-0 mt-2">
                      비회원 주문은 별도 경로로 이어지며, 선택한 상품과 수량을 미리 담아둡니다.
                    </p>
                  </section>
                ) : null}
              </div>
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

      <PublicReviewSection targetType="PRODUCT" targetId={productId} />
      <ProductQnaSection productId={productId} />
    </Container>
  );
}
