import { LinkButton } from "@/shared/ui/LinkButton";
import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { Alert, Container, Card, Button, Row, Col, Modal, Table } from "react-bootstrap";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { useCart } from "@/features/cart/useCart";
import { executePaymentFlow, type OrderPayload } from "@/features/payment";
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

export function CartPage() {
  const [showDiscardConfirm, setShowDiscardConfirm] = useState(false);
  const { isAuthenticated, user } = useCustomerAuth();
  const {
    items,
    totalAmount,
    isLoading,
    guestCartMergeIssue,
    retryGuestCartMerge,
    discardGuestCartMerge,
    updateQty,
    removeItem,
  } = useCart();
  const [fulfillment, setFulfillment] = useFulfillmentSelection(user?.name, user?.phone ?? undefined);
  const availableItems = items.filter((item) => item.available);
  const requiresMadeToOrderConsent = availableItems.some(
    (item) => item.productType === "MADE_TO_ORDER",
  );
  const consent = useMadeToOrderConsent(requiresMadeToOrderConsent);
  const loginHref = buildAuthPageHref("/login", { redirectTo: "/cart" });
  const checkout = useMutation({
    mutationFn: async () => {
      if (!user) {
        throw new Error("로그인이 필요합니다.");
      }
      const payload: OrderPayload = {
        type: "ORDER",
        userId: user.id,
        items: [],
        cartCheckout: true,
        madeToOrderConsent: consent.agreed,
        madeToOrderConsentVersion: consent.version,
        ...fulfillmentPayload(fulfillment),
      };
      await executePaymentFlow({
        context: "ORDER",
        payload,
        orderName: availableItems.length === 1
          ? `${availableItems[0]?.productName ?? "장바구니 상품"} 주문`
          : `장바구니 상품 ${availableItems.length}건`,
        customerKey: `member_${user.id}`,
        customerName: user.name,
        returnHint: { customerName: user.name },
      });
    },
    onError: consent.handleSubmissionError,
  });
  const consentVersionMismatch = isMadeToOrderConsentVersionMismatch(checkout.error);
  const mergeRecovery = guestCartMergeIssue && (
    <Alert variant="warning" className="mb-4">
      <Alert.Heading className="fs-6">로그인 전 장바구니 확인 필요</Alert.Heading>
      <p className="mb-3">{guestCartMergeIssue.message}</p>
      <div className="d-flex flex-wrap gap-2">
        {guestCartMergeIssue.canRetry && (
          <Button size="sm" variant="primary" onClick={retryGuestCartMerge}>
            다시 합치기
          </Button>
        )}
        <Button
          size="sm"
          variant="outline-danger"
          onClick={() => setShowDiscardConfirm(true)}
        >
          보류 항목 폐기
        </Button>
      </div>
      <small className="d-block mt-2">
        폐기하면 보류 요청에 포함된 로그인 전 수량만 이 기기에서 제거됩니다.
      </small>
    </Alert>
  );
  const discardConfirmModal = (
    <Modal
      show={showDiscardConfirm}
      onHide={() => setShowDiscardConfirm(false)}
      centered
    >
      <Modal.Header closeButton>
        <Modal.Title className="fs-6">보류 장바구니 항목 폐기</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        이 병합 요청에 포함된 로그인 전 수량을 이 기기에서 제거합니다. 이 작업은 되돌릴 수 없습니다.
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
          폐기
        </Button>
      </Modal.Footer>
    </Modal>
  );

  if (isLoading) {
    return <Container className="page-container"><LoadingSpinner /></Container>;
  }

  if (!isAuthenticated) {
    return (
      <Container className="page-container">
        <h2 className="mb-4">장바구니</h2>
        <Card className="text-center py-5">
          <Card.Body>
            <p className="mb-3">로그인하면 장바구니를 이용할 수 있습니다.</p>
            <LinkButton to={loginHref} variant="primary">로그인</LinkButton>
          </Card.Body>
        </Card>
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
                    <tr key={item.productId} className={item.available ? "" : "text-muted"}>
                      <td>
                        <Link to={`/products/${item.productId}`} className="text-decoration-none">
                          {item.productName || `상품 #${item.productId}`}
                        </Link>
                        <div className="small text-muted">{formatKRW(item.price)}</div>
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
                            disabled={item.qty <= 1}
                            onClick={() => updateQty(item.productId, item.qty - 1)}
                          >
                            -
                          </Button>
                          <span style={{ minWidth: 28, textAlign: "center" }}>{item.qty}</span>
                          <Button
                            variant="outline-secondary"
                            size="sm"
                            disabled={item.qty >= MAX_PRODUCT_QUANTITY}
                            onClick={() => updateQty(item.productId, item.qty + 1)}
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
                          onClick={() => removeItem(item.productId)}
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
              <OrderPriceSummary
                itemAmount={totalAmount}
                fulfillmentType={fulfillment.fulfillmentType}
                className="mb-3"
              />

              <div className="mb-3">
                <FulfillmentForm value={fulfillment} onChange={setFulfillment} />
              </div>

              <ErrorAlert error={consentVersionMismatch ? null : checkout.error} />
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
                  || !isFulfillmentComplete(fulfillment) || !consent.ready}
                onClick={handleCheckout}
              >
                {checkout.isPending ? "결제 준비 중..." : "결제하기"}
              </Button>
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </Container>
  );
}
