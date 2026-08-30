import { LinkButton } from "@/shared/ui/LinkButton";
import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Link } from "react-router";
import { Alert, Container, Card, Button, Row, Col, Modal, Table } from "react-bootstrap";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { useCart } from "@/features/cart/useCart";
import { executePaymentFlow, PaymentErrorAlert, PaymentMethodFields, useCheckoutSelection, type OrderPayload } from "@/features/payment";
import { LoadingSpinner, ErrorAlert, EmptyState } from "@/shared/ui";
import { formatKRW } from "@/shared/lib";
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
      <Container className="page-container">
        <h2 className="mb-4">장바구니</h2>
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
  const [showDiscardConfirm, setShowDiscardConfirm] = useState(false);
  const [issuedCouponId, setIssuedCouponId] = useState<number | null>(null);
  const [rewardAmount, setRewardAmount] = useState(0);
  const [checkoutSelection, setCheckoutSelection] = useCheckoutSelection();
  const { isAuthenticated, user } = useCustomerAuth();
  const {
    items,
    totalAmount,
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
  const availableItems = items.filter((item) => item.available);
  const requiresMadeToOrderConsent = isAuthenticated && availableItems.some(
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
        orderName: availableItems.length === 1
          ? `${availableItems[0]?.productName ?? "장바구니 상품"} 주문`
          : `장바구니 상품 ${availableItems.length}건`,
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
      <Container className="page-container">
        <h2 className="mb-4">장바구니</h2>
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
      <Container className="page-container">
        <h2 className="mb-4">장바구니</h2>
        {mergeRecovery}
        {discardConfirmModal}
        <EmptyState message="장바구니가 비어 있습니다." />
        <div className="text-center mt-3">
          <LinkButton to="/products" variant="outline-primary">상품 보러 가기</LinkButton>
        </div>
      </Container>
    );
  }

  const handleCheckout = async () => {
    if (!cartVersion || isItemMutationPending || isRefetching || guestCartMergeIssue) {
      return;
    }
    try {
      await checkout.mutateAsync();
    } catch {
      // error handled by React Query
    }
  };

  return (
    <Container className="page-container">
      <h2 className="mb-4">장바구니</h2>
      {mergeRecovery}
      {discardConfirmModal}
      {!isAuthenticated && cartError != null && (
        <ErrorAlert
          error={cartError}
          onRetry={refetch}
          retrying={isRefetching}
        />
      )}
      <ErrorAlert error={itemMutationError} />
      {isItemMutationPending && (
        <Alert variant="info" role="status" className="mb-3">
          장바구니 변경을 반영하고 있습니다.
        </Alert>
      )}

      <Row className="g-4">
        <Col lg={8}>
          <Card>
            <Card.Body className="p-0">
              <Table responsive className="mb-0">
                <thead>
                  <tr>
                    <th>상품</th>
                    <th className="text-center" style={{ width: 140 }}>수량</th>
                    <th className="text-end" style={{ width: 120 }}>소계</th>
                    <th style={{ width: 60 }}></th>
                  </tr>
                </thead>
                <tbody>
                  {items.map((item) => (
                    <tr key={item.cartItemId} className={item.available ? "" : "text-muted"}>
                      <td>
                        <Link to={`/products/${item.productId}`} className="text-decoration-none">
                          {item.productName || `상품 #${item.productId}`}
                        </Link>
                        <div className="small text-muted">{formatKRW(item.price)}</div>
                        {item.options.length > 0 && (
                          <div className="small text-muted mt-1">
                            {item.options.map((option) => (
                              <div key={`${option.sortOrder}-${option.groupName}`}>
                                {option.groupName}: {option.value}
                                {option.priceAdjustment > 0
                                  ? ` (+${formatKRW(option.priceAdjustment)})`
                                  : ""}
                              </div>
                            ))}
                          </div>
                        )}
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
                        {!item.available && (
                          <span className="badge bg-secondary">품절</span>
                        )}
                      </td>
                      <td className="text-center">
                        <div className="d-flex align-items-center justify-content-center gap-2">
                          <Button
                            variant="outline-secondary"
                            size="sm"
                            disabled={isItemMutationPending || item.qty <= 1}
                            onClick={() => {
                              void updateQty(item.cartItemId, item.qty - 1).catch(() => undefined);
                            }}
                          >
                            -
                          </Button>
                          <span style={{ minWidth: 28, textAlign: "center" }}>{item.qty}</span>
                          <Button
                            variant="outline-secondary"
                            size="sm"
                            disabled={isItemMutationPending || item.qty >= MAX_PRODUCT_QUANTITY}
                            onClick={() => {
                              void updateQty(item.cartItemId, item.qty + 1).catch(() => undefined);
                            }}
                          >
                            +
                          </Button>
                        </div>
                      </td>
                      <td className="text-end fw-semibold">{formatKRW(item.subtotal)}</td>
                      <td>
                        <Button
                          variant="link"
                          size="sm"
                          className="text-danger p-0"
                          disabled={isItemMutationPending}
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
            </Card.Body>
          </Card>
        </Col>

        <Col lg={4}>
          <Card className="store-purchase-card">
            <Card.Body>
              <h5 className="mb-3">주문 요약</h5>
              <div className="d-flex justify-content-between mb-2">
                <span className="text-muted">상품 수</span>
                <span>{items.length}종</span>
              </div>
              {!isAuthenticated ? (
                <>
                  <OrderPriceSummary
                    itemAmount={totalAmount}
                    fulfillmentType={fulfillment.fulfillmentType}
                    className="mb-3"
                  />
                  <Alert variant="light" className="border mb-3">
                    담은 상품은 이 기기에 유지됩니다. 로그인하면 회원 장바구니로 옮겨 결제할 수 있습니다.
                  </Alert>
                  <LinkButton
                    to={loginHref}
                    variant="primary"
                    size="lg"
                    className="w-100"
                  >
                    로그인하고 주문하기
                  </LinkButton>
                </>
              ) : (
                <>
                  <div className="border-top pt-3 mb-3">
                    <MemberOrderBenefits
                      productAmount={totalAmount}
                      fulfillmentType={fulfillment.fulfillmentType}
                      selectedCouponId={issuedCouponId}
                      rewardPointsToUse={rewardAmount}
                      disabled={checkout.isPending || isItemMutationPending || isRefetching}
                      onCouponChange={setIssuedCouponId}
                      onRewardPointsChange={setRewardAmount}
                    />
                  </div>

                  <div className="mb-3">
                    <FulfillmentForm value={fulfillment} onChange={setFulfillment} />
                  </div>

                  <PaymentMethodFields value={checkoutSelection} onChange={setCheckoutSelection} disabled={checkout.isPending} />
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
                    variant="primary"
                    size="lg"
                    className="w-100"
                    disabled={checkout.isPending || availableItems.length === 0
                      || !cartVersion
                      || isItemMutationPending || isRefetching || guestCartMergeIssue !== null
                      || !isFulfillmentComplete(fulfillment) || !consent.ready}
                    onClick={handleCheckout}
                  >
                    {checkout.isPending ? "결제 준비 중..." : "결제하기"}
                  </Button>
                </>
              )}
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </Container>
  );
}
