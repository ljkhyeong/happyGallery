import { LinkButton } from "@/shared/ui/LinkButton";
import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Link } from "react-router";
import { Alert, Container, Button, Modal, Table, Form } from "react-bootstrap";
import { ShoppingBag } from "lucide-react";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { productQuantities, productSkuKey } from "@/features/product/purchaseStock";
import type { CartItemIdentifier, CartItemView } from "@/features/cart/guestCartView";
import { useCart } from "@/features/cart/useCart";
import { CartQuantityError } from "@/features/cart/useGuestCart";
import { executePaymentFlow, PaymentErrorAlert, PaymentMethodFields, useCheckoutSelection, type OrderPayload } from "@/features/payment";
import { CheckoutLayout, CheckoutPanel, CheckoutSummary, ErrorAlert, LoadingSpinner, PageHeader } from "@/shared/ui";
import { formatKRW } from "@/shared/lib";
import {
  FulfillmentForm,
  fulfillmentPayload,
  isFulfillmentComplete,
  useFulfillmentSelection,
} from "@/features/order/FulfillmentForm";
import { OrderPriceSummary } from "@/features/order/OrderPriceSummary";
import { OrderOptionList } from "@/features/order/OrderOptionList";
import { MadeToOrderConsent } from "@/features/order/MadeToOrderConsent";
import {
  isMadeToOrderConsentVersionMismatch,
  useMadeToOrderConsent,
} from "@/features/order/useMadeToOrderConsent";
import { buildAuthPageHref } from "@/features/customer-auth/navigation";
import { MAX_PRODUCT_QUANTITY } from "@/shared/validation/productQuantity";
import { ProductPurchaseTerms } from "@/features/product/ProductPurchaseTerms";
import { isCartSnapshotConflict } from "@/features/cart/cartSnapshot";
import { MemberOrderBenefits } from "@/features/order-benefit/MemberOrderBenefits";

export function CartPage() {
  const {
    status,
    isLoading,
    sessionVersion,
  } = useCustomerAuth();
  if (status === "error") {
    return (
      <Container className="page-container checkout-page">
        <PageHeader title="장바구니" description="담은 작품을 확인하고 원하는 작품만 골라 한 번에 결제하세요." />
        <LoadingSpinner text="로그인 상태 확인을 기다리고 있습니다." />
      </Container>
    );
  }
  if (isLoading) {
    return <Container className="page-container"><LoadingSpinner /></Container>;
  }
  return <CartContent key={sessionVersion} />;
}

function CartContent() {
  const [excludedItemIds, setExcludedItemIds] = useState<Set<CartItemIdentifier>>(new Set());
  const [showDiscardConfirm, setShowDiscardConfirm] = useState(false);
  const [issuedCouponId, setIssuedCouponId] = useState<number | null>(null);
  const [rewardAmount, setRewardAmount] = useState(0);
  const [checkoutSelection, setCheckoutSelection] = useCheckoutSelection();
  const { isAuthenticated, user } = useCustomerAuth();
  const {
    items,
    totalAmount: cartTotalAmount,
    cartVersion,
    isLoading,
    error: cartError,
    isRefetching,
    refetch,
    itemMutationError,
    isItemMutationPending,
    guestCartMergeIssue,
    retryGuestCartMerge,
    discardGuestCartMerge,
    updateQty,
    removeItem,
  } = useCart();
  const [fulfillment, setFulfillment] = useFulfillmentSelection(
    user?.name,
    user?.phone ?? undefined,
  );
  const canSelect = (item: CartItemView) => item.available
    || (isAuthenticated && item.qty <= item.availableQuantity);
  const availableItems = items.filter(canSelect);
  const selectedItems = availableItems.filter((item) => !excludedItemIds.has(item.cartItemId));
  const selectedQuantities = productQuantities(selectedItems);
  const stockExceededItems = selectedItems.filter((item, index) =>
    (selectedQuantities.get(productSkuKey(item)) ?? 0) > item.availableQuantity
    && selectedItems.findIndex((other) => productSkuKey(other) === productSkuKey(item)) === index);
  const totalAmount = isAuthenticated
    ? selectedItems.reduce((sum, item) => sum + item.subtotal, 0)
    : cartTotalAmount;
  const requiresMadeToOrderConsent = isAuthenticated && selectedItems.some(
    (item) => item.productType === "MADE_TO_ORDER",
  );
  const consent = useMadeToOrderConsent(requiresMadeToOrderConsent);
  const loginHref = buildAuthPageHref("/login", { redirectTo: "/cart" });
  const checkout = useMutation({
    mutationFn: async () => {
      if (!user) {
        throw new Error("로그인이 필요합니다.");
      }
      if (!cartVersion) {
        throw new Error("장바구니 최신 정보를 다시 확인해 주세요.");
      }
      const payload: OrderPayload = {
        type: "ORDER",
        userId: user.id,
        items: [],
        cartCheckout: true,
        expectedCartVersion: cartVersion,
        selectedCartItemIds: selectedItems.map((item) => {
          if (typeof item.cartItemId !== "number") throw new Error("장바구니를 다시 불러와 주세요.");
          return item.cartItemId;
        }),
        madeToOrderConsent: consent.agreed,
        madeToOrderConsentVersion: consent.version,
        ...(issuedCouponId === null ? {} : { issuedCouponId }),
        rewardAmount,
        ...fulfillmentPayload(fulfillment),
      };
      await executePaymentFlow({
        checkoutSelection,
        context: "ORDER",
        payload,
        orderName: selectedItems.length === 1
          ? `${selectedItems[0]?.productName ?? "장바구니 상품"} 주문`
          : `장바구니 상품 ${selectedItems.length}건`,
        customerKey: `member_${user.id}`,
        customerName: user.name,
        returnHint: { customerName: user.name, returnPath: "/cart" },
      });
    },
    onError: (error) => {
      consent.handleSubmissionError(error);
      if (isCartSnapshotConflict(error)) {
        refetch();
      }
    },
  });
  const changeSelection = (next: Set<CartItemIdentifier>) => {
    setExcludedItemIds(next);
    setIssuedCouponId(null);
    setRewardAmount(0);
    checkout.reset();
  };
  const selectionDisabled = checkout.isPending || isItemMutationPending || isRefetching;
  const consentVersionMismatch = isMadeToOrderConsentVersionMismatch(checkout.error);
  const cartSnapshotConflict = isCartSnapshotConflict(checkout.error);
  const mergeRecovery = guestCartMergeIssue && (
    <Alert variant="warning" className="mb-4">
      <Alert.Heading className="fs-6">로그인 전 장바구니 확인 필요</Alert.Heading>
      <p className="mb-3">{guestCartMergeIssue.message}</p>
      <div className="d-flex flex-wrap gap-2">
        {guestCartMergeIssue.canRetry && (
          <Button size="sm" variant="primary" onClick={retryGuestCartMerge}>
            다시 불러오기
          </Button>
        )}
        <Button
          size="sm"
          variant="outline-danger"
          onClick={() => setShowDiscardConfirm(true)}
        >
          이 기기에서 해당 상품 제거
        </Button>
      </div>
      <small className="d-block mt-2">
        제거하면 로그인 전에 담은 해당 상품 수량만 이 기기에서 사라집니다.
      </small>
    </Alert>
  );
  const discardConfirmModal = (
    <Modal
      show={showDiscardConfirm}
      aria-labelledby="held-cart-discard-title"
      onHide={() => setShowDiscardConfirm(false)}
      centered
    >
      <Modal.Header closeButton>
        <Modal.Title id="held-cart-discard-title" className="fs-6">
          로그인 전에 담은 상품 제거
        </Modal.Title>
      </Modal.Header>
      <Modal.Body>
        다른 계정으로 로그인하기 전에 담은 상품 수량을 이 기기에서 제거합니다. 이 작업은 되돌릴 수 없습니다.
      </Modal.Body>
      <Modal.Footer>
        <Button variant="outline-secondary" onClick={() => setShowDiscardConfirm(false)}>
          취소
        </Button>
        <Button
          variant="danger"
          onClick={() => {
            discardGuestCartMerge();
            setShowDiscardConfirm(false);
          }}
        >
          상품 제거
        </Button>
      </Modal.Footer>
    </Modal>
  );

  if (isLoading) {
    return <Container className="page-container"><LoadingSpinner /></Container>;
  }

  if (cartError && isAuthenticated) {
    return (
      <Container className="page-container checkout-page">
        <PageHeader title="장바구니" description="담은 작품을 확인하고 원하는 작품만 골라 한 번에 결제하세요." />
        {mergeRecovery}
        {discardConfirmModal}
        <ErrorAlert
          error={cartError}
          onRetry={refetch}
          retrying={isRefetching}
        />
      </Container>
    );
  }

  if (items.length === 0) {
    return (
      <Container className="page-container checkout-page">
        <PageHeader title="장바구니" description="담은 작품을 확인하고 원하는 작품만 골라 한 번에 결제하세요." />
        {mergeRecovery}
        {discardConfirmModal}
        <section className="cart-empty">
          <ShoppingBag size={36} strokeWidth={1.4} aria-hidden="true" />
          <h2>장바구니가 비어 있습니다.</h2>
          <p>공방에서 만든 작품을 둘러보고 마음에 드는 작품을 담아 보세요.</p>
          <div className="cart-empty-actions">
            <LinkButton to="/products" variant="primary">작품 둘러보기</LinkButton>
            <LinkButton to="/classes" variant="outline-dark">클래스 둘러보기</LinkButton>
          </div>
        </section>
      </Container>
    );
  }

  const handleCheckout = async () => {
    if (!cartVersion || selectedItems.length === 0 || stockExceededItems.length > 0 || isItemMutationPending || isRefetching || guestCartMergeIssue) {
      return;
    }
    try {
      await checkout.mutateAsync();
    } catch {
      // error handled by React Query
    }
  };

  // 결제하기가 비활성인 이유를 화면 순서대로 알려 준다. 수량 초과·장바구니 변경 중 안내는 각 알림이 따로 보인다.
  const remainingSteps = isAuthenticated ? [
    selectedItems.length === 0 && "작품 선택",
    !fulfillment.fulfillmentType && "수령 방법",
    fulfillment.fulfillmentType === "SHIPPING" && !isFulfillmentComplete(fulfillment) && "배송지 입력",
    !consent.ready && "주문제작 조건 동의",
  ].filter((value): value is string => Boolean(value)) : [];

  const checkoutButton = isAuthenticated ? (
    <Button
      variant="primary"
      size="lg"
      className="w-100"
      disabled={checkout.isPending || selectedItems.length === 0 || stockExceededItems.length > 0
        || !cartVersion
        || isItemMutationPending || isRefetching || guestCartMergeIssue !== null
        || !isFulfillmentComplete(fulfillment) || !consent.ready}
      onClick={handleCheckout}
    >
      {checkout.isPending ? "결제 준비 중..." : "결제하기"}
    </Button>
  ) : (
    <LinkButton to={loginHref} variant="primary" size="lg" className="w-100">
      로그인하고 주문하기
    </LinkButton>
  );

  return (
    <Container className="page-container checkout-page">
      <PageHeader title="장바구니" description="담은 작품을 확인하고 원하는 작품만 골라 한 번에 결제하세요." />
      {mergeRecovery}
      {discardConfirmModal}
      {!isAuthenticated && cartError != null && (
        <ErrorAlert
          error={cartError}
          onRetry={refetch}
          retrying={isRefetching}
        />
      )}
      {itemMutationError instanceof CartQuantityError
        ? <Alert variant="warning">{itemMutationError.message}</Alert>
        : <ErrorAlert error={itemMutationError} />}
      {isItemMutationPending && (
        <Alert variant="info" role="status" className="mb-3">
          장바구니 변경을 반영하고 있습니다.
        </Alert>
      )}

      <CheckoutLayout
        summary={(
          <CheckoutSummary
            label="주문 요약"
            className="store-purchase-card"
            hint={remainingSteps.length > 0 ? `남은 단계: ${remainingSteps.join(" · ")}` : undefined}
            action={checkoutButton}
            note={isAuthenticated
              ? "선택하지 않은 작품은 결제 후에도 장바구니에 남습니다."
              : "담은 작품은 이 기기에 유지됩니다. 로그인하면 회원 장바구니로 옮겨 결제할 수 있습니다."}
          >
            <h2 className="checkout-summary-title">주문 요약</h2>
            <div className="d-flex justify-content-between mb-3">
              <span className="text-muted-soft">{isAuthenticated ? "선택 작품" : "담은 작품"}</span>
              <span>{isAuthenticated ? selectedItems.length : items.length}종</span>
            </div>
            {!isAuthenticated ? (
              <OrderPriceSummary
                itemAmount={totalAmount}
                fulfillmentType={fulfillment.fulfillmentType}
              />
            ) : (
              <>
                {selectedItems.length === 0 && <Alert variant="info">구매할 작품을 선택해 주세요.</Alert>}
                {stockExceededItems.map((item) => <Alert key={productSkuKey(item)} variant="warning">
                  {item.productName}: 같은 상품·옵션은 합계 {item.availableQuantity}개까지 구매할 수 있습니다.
                  현재 {selectedQuantities.get(productSkuKey(item))}개를 선택했습니다. 선택을 줄이거나 수량을 조정해 주세요.
                </Alert>)}
                <MemberOrderBenefits
                  productAmount={totalAmount}
                  fulfillmentType={fulfillment.fulfillmentType}
                  selectedCouponId={issuedCouponId}
                  rewardPointsToUse={rewardAmount}
                  disabled={checkout.isPending || isItemMutationPending || isRefetching}
                  onCouponChange={setIssuedCouponId}
                  onRewardPointsChange={setRewardAmount}
                />
                <PaymentErrorAlert
                  error={consentVersionMismatch || cartSnapshotConflict ? null : checkout.error}
                />
                {cartSnapshotConflict && (
                  <Alert variant="warning" role="alert" className="mb-3">
                    {isRefetching
                      ? "장바구니 내용이 변경되어 최신 정보를 다시 불러오고 있습니다."
                      : "장바구니 내용이 변경되어 최신 정보로 갱신했습니다. 수량과 금액을 다시 확인한 뒤 결제를 진행해 주세요."}
                  </Alert>
                )}
                {!cartVersion && (
                  <Alert variant="warning" role="alert" className="mb-3">
                    <div>장바구니 최신 정보를 확인할 수 없어 결제를 진행할 수 없습니다.</div>
                    <Button
                      type="button"
                      variant="outline-dark"
                      size="sm"
                      className="mt-2"
                      disabled={isRefetching}
                      onClick={refetch}
                    >
                      {isRefetching ? "다시 확인 중..." : "장바구니 다시 확인"}
                    </Button>
                  </Alert>
                )}
              </>
            )}
          </CheckoutSummary>
        )}
      >
        <CheckoutPanel step={isAuthenticated ? 1 : undefined} title="담은 작품" meta={`${items.length}종`}>
          {isAuthenticated && (
            <div className="cart-select-all">
              <Form.Check id="cart-select-all" label={`전체 선택 (${selectedItems.length}/${availableItems.length})`}
                checked={availableItems.length > 0 && selectedItems.length === availableItems.length}
                disabled={selectionDisabled || availableItems.length === 0}
                onChange={(event) => changeSelection(event.target.checked
                  ? new Set() : new Set(availableItems.map((item) => item.cartItemId)))} />
            </div>
          )}
          <Table className="cart-table mb-0">
            <thead>
              <tr>
                <th>작품</th>
                <th className="text-center">수량</th>
                <th className="text-end">소계</th>
                <th><span className="visually-hidden">삭제</span></th>
              </tr>
            </thead>
            <tbody>
              {items.map((item) => (
                <tr key={item.cartItemId} className={canSelect(item) ? undefined : "is-unavailable"}>
                  <td className="cart-item-main">
                    <div className="cart-item-title">
                      {isAuthenticated && <Form.Check
                        id={`cart-select-${item.cartItemId}`}
                        aria-label={`${item.productName} 선택`}
                        checked={canSelect(item) && !excludedItemIds.has(item.cartItemId)}
                        disabled={selectionDisabled || !canSelect(item)}
                        onChange={(event) => {
                          const next = new Set(excludedItemIds);
                          if (event.target.checked) next.delete(item.cartItemId);
                          else next.add(item.cartItemId);
                          changeSelection(next);
                        }} />}
                      <div>
                        <Link to={`/products/${item.productId}`}>
                          {item.productName || `상품 #${item.productId}`}
                        </Link>
                        <div className="small text-muted-soft">{formatKRW(item.price)}</div>
                      </div>
                    </div>
                    <OrderOptionList options={item.options} />
                    {item.productType && (
                      <div className="mt-2">
                        <ProductPurchaseTerms
                          productName={item.productName}
                          type={item.productType}
                          specification={item.specification}
                          careInstructions={item.careInstructions}
                          productionLeadDays={item.productionLeadDays}
                          compact
                        />
                      </div>
                    )}
                    {item.quantityWarning && <div className="small text-danger">{item.quantityWarning}</div>}
                    {!canSelect(item) && !item.quantityWarning && (
                      <div>
                        <span className="badge bg-secondary">구매 불가</span>
                        <div className="small text-danger">
                          재고 또는 판매 옵션이 변경되었습니다. 수량을 줄이거나 삭제해 주세요.
                        </div>
                      </div>
                    )}
                  </td>
                  <td className="cart-item-qty">
                    <div className="cart-qty-stepper">
                      <Button
                        variant="outline-secondary"
                        size="sm"
                        disabled={checkout.isPending || isItemMutationPending || item.qty <= 1}
                        onClick={() => {
                          void updateQty(item.cartItemId, item.qty - 1).catch(() => undefined);
                        }}
                      >
                        -
                      </Button>
                      <span>{item.qty}</span>
                      <Button
                        variant="outline-secondary"
                        size="sm"
                        disabled={checkout.isPending || isItemMutationPending || item.qty >= (item.maxQuantity ?? MAX_PRODUCT_QUANTITY)}
                        onClick={() => {
                          void updateQty(item.cartItemId, item.qty + 1).catch(() => undefined);
                        }}
                      >
                        +
                      </Button>
                    </div>
                  </td>
                  <td className="cart-item-subtotal">{formatKRW(item.subtotal)}</td>
                  <td className="cart-item-remove">
                    <Button
                      variant="link"
                      size="sm"
                      disabled={checkout.isPending || isItemMutationPending}
                      onClick={() => {
                        void removeItem(item.cartItemId).catch(() => undefined);
                      }}
                    >
                      삭제
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        </CheckoutPanel>

        {isAuthenticated && (
          <>
            <CheckoutPanel step={2} title="수령 방법">
              <FulfillmentForm value={fulfillment} onChange={setFulfillment} />
            </CheckoutPanel>
            <CheckoutPanel step={3} title="결제 수단">
              <PaymentMethodFields
                value={checkoutSelection}
                onChange={setCheckoutSelection}
                disabled={checkout.isPending}
                showLegend={false}
              />
            </CheckoutPanel>
            {requiresMadeToOrderConsent && (
              <CheckoutPanel step={4} title="동의">
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
              </CheckoutPanel>
            )}
          </>
        )}
      </CheckoutLayout>
    </Container>
  );
}
