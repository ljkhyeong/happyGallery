import { FavoriteButton } from "@/features/my/Favorites";
import { LinkButton } from "@/shared/ui/LinkButton";
import { useEffect, useMemo, useRef, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router";
import { useMutation } from "@tanstack/react-query";
import { Alert, Container, Card, Button, Form } from "react-bootstrap";
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
import { ErrorAlert, useToast } from "@/shared/ui";
import {
  formatKRW,
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
import { useMadeToOrderConsent } from "@/features/order/useMadeToOrderConsent";
import { queryKeys, runForCurrentCustomer, useLoaderBackedQuery } from "@/shared/api";
import { ProductMediaPlaceholder } from "@/features/product/ProductMediaPlaceholder";
import { ProductPurchaseGuide } from "@/features/product/ProductPurchaseGuide";
import { ProductPurchaseTerms } from "@/features/product/ProductPurchaseTerms";
import { sumQuantitiesByVariant } from "@/features/product/purchaseQuantity";
import { productSelectionView } from "@/features/product/productSelectionView";
import { productQuantityLimit } from "@/features/product/purchaseStock";
import { RestockAlertButton } from "@/features/product/RestockAlertButton";
import { productDetailHref } from "@/features/product/navigation";
import { saveGuestOrderDraft } from "@/features/order/guestOrderDraft";
import { PublicReviewSection } from "@/features/review/PublicReviewSection";
import type { ProductDetailResponse } from "@/generated/api/product";
import {
  ProductPurchaseOptions,
  type PurchaseLine,
} from "@/features/product/ProductPurchaseOptions";

export function ProductDetailPage({ initialProduct }: { initialProduct: ProductDetailResponse }) {
  const { sessionVersion } = useCustomerAuth();
  const [searchParams] = useSearchParams();
  const variantId = searchParams.get("variantId");
  return <ProductDetailContent key={`${sessionVersion}:${initialProduct.id}:${variantId ?? ""}`} initialProduct={initialProduct} variantId={variantId} />;
}

function ProductDetailContent({ initialProduct, variantId }: { initialProduct: ProductDetailResponse; variantId: string | null }) {
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
  } = useLoaderBackedQuery({
    queryKey: productQueryKey,
    queryFn: () => fetchProduct(productId),
    staleTime: PUBLIC_DATA_STALE_TIME,
  }, initialProduct);
  const requiresMadeToOrderConsent = product.type === "MADE_TO_ORDER";
  const consent = useMadeToOrderConsent(requiresMadeToOrderConsent);

  const orderMutation = useMutation({
    mutationFn: async () => {
      if (!user) throw new Error("로그인이 필요합니다.");
      const items = (product.optionGroups.length || purchaseLines.length)
        ? purchaseLines.map((line) => ({
          productId,
          productVariantId: line.productVariantId,
          textInputs: line.textInputs,
          qty: line.qty,
        }))
        : [{
          productId,
          productVariantId: productSelectionView(product, {}).productVariantId,
          textInputs: [],
          qty,
        }];
      const payload: OrderPayload = {
        type: "ORDER",
        userId: user.id,
        name: user.name,
        items,
        cartCheckout: false,
        issuedCouponId,
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
          // 결제를 마치지 못하고 돌아오면 옵션을 하나만 골랐을 때 그 옵션을 다시 골라 둔다.
          returnPath: productDetailHref(productId,
            product.optionGroups.length > 0 && items.length === 1 ? items[0]!.productVariantId : null),
        },
      });
    },
    onError: consent.handleSubmissionError,
  });
  const cartMutation = useMutation({
    mutationFn: () => runForCurrentCustomer(
      async () => {
        const items = (product.optionGroups.length || purchaseLines.length)
          ? purchaseLines.map((line) => ({
            productId, productVariantId: line.productVariantId, textInputs: line.textInputs, qty: line.qty,
          }))
          : [{ productId, productVariantId: productSelectionView(product, {}).productVariantId, textInputs: [], qty }];
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

  if (error) return <Container className="page-container"><ErrorAlert error={error} /></Container>;

  const hasConfiguredOptions = product.optionGroups.length > 0 || purchaseLines.length > 0;
  const linkedVariant = product.variants.find((variant) => variant.active && String(variant.id) === variantId);
  const selectionViews = purchaseLines.map((line) => productSelectionView(product, line));
  const selectionChanged = selectionViews.some((view, index) => {
    const initial = productSelectionView(initialProduct, purchaseLines[index]!);
    return view.unitPrice !== initial.unitPrice || view.label !== initial.label
      || view.configurationValid !== initial.configurationValid;
  });
  const selectedQuantity = hasConfiguredOptions
    ? purchaseLines.reduce((sum, line) => sum + line.qty, 0)
    : qty;
  const defaultSelection = productSelectionView(product, {});
  const defaultQuantityLimit = productQuantityLimit(product, defaultSelection.productVariantId);
  const itemAmount = hasConfiguredOptions
    ? purchaseLines.reduce((sum, line, index) => sum + selectionViews[index]!.unitPrice * line.qty, 0)
    : defaultSelection.unitPrice * qty;
  const selectedQuantities = sumQuantitiesByVariant(purchaseLines);
  const canBuy = product.available
    && selectedQuantity >= 1
    && (hasConfiguredOptions || selectedQuantity <= defaultQuantityLimit)
    && (hasConfiguredOptions || defaultSelection.configurationValid)
    && selectionViews.every((view) => view.configurationValid)
    && [...selectedQuantities].every(([variantId, quantity]) => quantity <= productQuantityLimit(product, variantId));
  const canCheckout = canBuy && isFulfillmentComplete(fulfillment);
  const guestFallbackPath = `/orders/new?productId=${productId}&qty=${qty}`;
  const memberRedirectPath = productDetailHref(productId, linkedVariant?.id);
  const loginHref = buildAuthPageHref("/login", { redirectTo: memberRedirectPath });
  const signupHref = buildAuthPageHref("/signup", { redirectTo: memberRedirectPath });
  // 주문 제작은 고정 사양 안내가 늘 있고, 재고 상품은 사양·관리 방법이 있을 때만 작품 정보 구역을 둔다.
  const hasProductInfo = product.type === "MADE_TO_ORDER" || Boolean(product.specification)
    || Boolean(product.careInstructions) || product.productionLeadDays != null;

  return (
    <Container className="page-container store-product-page">
      <nav className="store-detail-breadcrumb" aria-label="경로">
        <Link to="/" className="store-detail-breadcrumb-link">홈</Link>
        <span aria-hidden="true">/</span>
        <Link to="/products" className="store-detail-breadcrumb-link">작품</Link>
        {product.category && (
          <>
            <span aria-hidden="true">/</span>
            <Link
              to={`/products?${new URLSearchParams({ category: product.category })}`}
              className="store-detail-breadcrumb-link"
            >
              {product.category}
            </Link>
          </>
        )}
        <span aria-hidden="true">/</span>
        <span className="store-detail-breadcrumb-current" aria-current="page">{product.name}</span>
      </nav>

      <div className="store-product-layout">
        <div className="store-product-gallery">
          <figure className="store-detail-media">
            {product.imageUrl ? (
              <img src={product.imageUrl} alt={product.name} fetchPriority="high" />
            ) : (
              <ProductMediaPlaceholder size="detail" />
            )}
          </figure>
        </div>

        <div className="store-product-buy">
          <header className="store-product-intro">
            <div className="store-detail-meta">
              <span className="store-detail-kicker">
                {PRODUCT_TYPE_LABEL[product.type] ?? "상품 종류 확인 필요"}
              </span>
              {product.category && (
                <Link
                  to={`/products?${new URLSearchParams({ category: product.category })}`}
                  className="store-detail-category"
                >
                  {product.category}
                </Link>
              )}
              <span className={`store-detail-availability ${product.available ? "is-available" : "is-sold-out"}`}>
                {product.available ? "주문 가능" : "품절"}
              </span>
            </div>
            <h1 className="store-detail-title">{product.name}</h1>
            <div className="store-detail-price-block">
              {/* 옵션에 따라 금액이 더해질 때만 시작 금액임을 밝힌다. */}
              {product.optionGroups.length > 0 && <span>기본가</span>}
              <strong className="store-detail-price">{formatKRW(product.price)}</strong>
            </div>
            {product.type === "MADE_TO_ORDER" && product.productionLeadDays != null && (
              <p className="store-detail-lead">제작 기간은 결제 후 약 {product.productionLeadDays}일입니다.</p>
            )}
            {product.description && <p className="store-detail-description">{product.description}</p>}
            <FavoriteButton type="PRODUCT" targetId={product.id} />
          </header>

          <Card ref={purchasePanelRef} className="purchase-panel store-purchase-card store-order-sheet">
            <Card.Body className="p-4 p-xl-5">
              <h2 className="visually-hidden">구매 옵션</h2>
              <section className="store-order-sheet-step" aria-labelledby="product-option-title">
                {/* 옵션이 없으면 수량 칸만 있어 제목은 화면 읽기용으로만 둔다. */}
                <h3
                  id="product-option-title"
                  className={hasConfiguredOptions ? "store-order-sheet-step-heading" : "visually-hidden"}
                >
                  {hasConfiguredOptions ? "옵션 선택" : "수량 선택"}
                </h3>
                {selectionChanged && (
                  <Alert variant="info" className="py-2">
                    상품 가격이나 옵션이 변경되었습니다. 옵션과 금액을 다시 확인해 주세요.
                  </Alert>
                )}
                {variantId !== null && !linkedVariant && (
                  <Alert variant="warning" className="py-2">
                    해당 옵션은 현재 판매하지 않습니다. 현재 상품의 옵션과 가격을 확인해 주세요.
                  </Alert>
                )}
                {hasConfiguredOptions ? (
                  <ProductPurchaseOptions
                    product={product}
                    initialVariant={linkedVariant}
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
                        type="number" inputMode="numeric"
                        min={1}
                        max={Math.max(1, defaultQuantityLimit)}
                        value={qty}
                        onChange={(e) => {
                          const v = Number(e.target.value);
                          if (Number.isInteger(v) && v >= 1 && (v < qty || v <= defaultQuantityLimit)) setQty(v);
                        }}
                        className="text-center store-purchase-qty-input"
                      />
                      <Button
                        variant="outline-dark"
                        size="sm"
                        disabled={qty >= defaultQuantityLimit}
                        onClick={() => setQty((q) => Math.min(defaultQuantityLimit, q + 1))}
                        aria-label="수량 증가"
                        className="store-purchase-qty-btn"
                      >
                        +
                      </Button>
                    </div>
                    {qty > defaultQuantityLimit && product.available && (
                      <Form.Text className="text-danger">현재 {defaultQuantityLimit}개까지 주문할 수 있습니다. 수량을 줄여 주세요.</Form.Text>
                    )}
                  </div>
                )}
              </section>

              <div className="store-purchase-summary">
                {!hasConfiguredOptions && !product.available && (
                  <RestockAlertButton productId={product.id} productVariantId={product.type === "MADE_TO_ORDER" ? product.variants.find((variant) => variant.active)?.id ?? null : null} />
                )}
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
                  <h3 id="product-order-title" className="store-order-sheet-step-heading">받는 방법</h3>
                  <FulfillmentForm value={fulfillment} onChange={setFulfillment} />
                </section>
              )}

              <div className="store-order-sheet-actions">
                <PaymentErrorAlert error={consent.paymentError(orderMutation.error)} />
                {cartMutation.error instanceof CartQuantityError ? (
                  <Alert variant="danger">{cartMutation.error.message}</Alert>
                ) : (
                  <ErrorAlert error={cartMutation.error} />
                )}

                {!authLoading && isAuthenticated ? (
                  <>
                    <PaymentMethodFields value={checkoutSelection} onChange={setCheckoutSelection} disabled={orderMutation.isPending} />
                    <MadeToOrderConsent consent={consent} />
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
                      className="w-100"
                      disabled={!canBuy || cartMutation.isPending}
                      onClick={() => cartMutation.mutate()}
                    >
                      {cartMutation.isPending ? "담는 중..." : "장바구니 담기"}
                    </Button>
                  </>
                ) : !authLoading ? (
                  <div>
                    <Button
                      variant="dark"
                      size="lg"
                      className="w-100 mb-2 store-purchase-btn-primary"
                      disabled={!canBuy}
                      onClick={() => navigate(loginHref)}
                    >
                      로그인 후 구매하기
                    </Button>
                    {/* 비회원 주문도 장바구니와 같은 크기로 보여 찾기 쉽게 하고, 회원가입은 혜택과 함께 문장 링크로 둔다. */}
                    <div className="store-purchase-secondary">
                      {hasConfiguredOptions ? (
                        <Button
                          variant="outline-dark"
                          disabled={!canBuy}
                          onClick={() => {
                            const draftId = saveGuestOrderDraft(productId, purchaseLines.map((line) => ({
                              productId, productVariantId: line.productVariantId, textInputs: line.textInputs, qty: line.qty,
                            })));
                            if (!draftId) {
                              toast.show("주문 정보를 저장하지 못했습니다. 브라우저 저장소 설정을 확인한 뒤 다시 시도해 주세요.");
                              return;
                            }
                            navigate(`/orders/new?productId=${productId}&draft=options&draftId=${draftId}`);
                          }}
                        >
                          비회원 주문하기
                        </Button>
                      ) : (
                        <LinkButton
                          to={guestFallbackPath}
                          aria-disabled={!canBuy}
                          tabIndex={canBuy ? undefined : -1}
                          onClick={(event) => { if (!canBuy) event.preventDefault(); }}
                          variant="outline-dark"
                          className={canBuy ? undefined : "disabled"}
                        >
                          비회원 주문하기
                        </LinkButton>
                      )}
                      <Button
                        variant="outline-dark"
                        disabled={!canBuy || cartMutation.isPending}
                        onClick={() => cartMutation.mutate()}
                      >
                        {cartMutation.isPending ? "담는 중..." : "장바구니 담기"}
                      </Button>
                    </div>
                    <p className="store-purchase-helper mb-0 mt-2">
                      회원은 쿠폰과 결제 금액 1% 적립을 받을 수 있습니다.{" "}
                      <Link to={signupHref}>회원가입 후 구매하기</Link>
                    </p>
                  </div>
                ) : null}
              </div>
            </Card.Body>
          </Card>
        </div>
      </div>

      {/* 상세 아래 구역으로 바로 가는 탭. 화면 위에 붙어 따라오며 각 구역은 탭 높이만큼 띄워 멈춘다. */}
      <nav className="store-detail-tabs" aria-label="작품 상세 구역">
        {hasProductInfo && <a href="#product-info">작품 정보</a>}
        <a href="#product-delivery">배송·교환</a>
        <a href="#product-reviews">후기</a>
        <a href="#product-qna">문의</a>
      </nav>

      {hasProductInfo && (
        <section id="product-info" className="store-detail-section" aria-labelledby="product-information-title">
          <h2 id="product-information-title" className="store-detail-section-title">작품 정보</h2>
          <div className="store-detail-terms">
            <ProductPurchaseTerms
              productName={product.name}
              type={product.type}
              specification={product.specification}
              careInstructions={product.careInstructions}
              productionLeadDays={product.productionLeadDays}
            />
          </div>
        </section>
      )}

      <section id="product-delivery" className="store-detail-section" aria-labelledby="product-delivery-title">
        <h2 id="product-delivery-title" className="store-detail-section-title">배송·교환</h2>
        <ProductPurchaseGuide type={product.type} />
      </section>

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

      <div id="product-reviews" className="store-detail-anchor">
        <PublicReviewSection targetType="PRODUCT" targetId={productId} />
      </div>
      <div id="product-qna" className="store-detail-anchor">
        <ProductQnaSection productId={productId} />
      </div>
    </Container>
  );
}
