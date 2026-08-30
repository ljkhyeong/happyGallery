import { LinkButton } from "@/shared/ui/LinkButton";
import { useEffect, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Container, Card, Button, Form, Badge, Alert } from "react-bootstrap";
import { useSearchParams } from "react-router";
import { PhoneVerificationStep } from "@/features/booking-create/PhoneVerificationStep";
import { trackClientEvent } from "@/features/monitoring/api";
import { OrderItemsForm } from "@/features/order/OrderItemsForm";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import {
  executePaymentFlow,
  PaymentMethodFields,
  PaymentErrorAlert,
  useCheckoutSelection,
  type OrderPayload,
} from "@/features/payment";
import { LoadingSpinner } from "@/shared/ui";
import type { OrderItemInput } from "@/shared/types";
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
import type { ProductType } from "@/shared/types/product";
import { buildAuthPageHref } from "@/features/customer-auth/navigation";
import { PolicyConsentFields } from "@/features/policy-consent/PolicyConsentFields";
import { usePolicyAcceptance } from "@/features/policy-consent/usePolicyAcceptance";
import { MAX_PRODUCT_QUANTITY } from "@/shared/validation/productQuantity";
import { MemberOrderBenefits } from "@/features/order-benefit/MemberOrderBenefits";
import { ApiError } from "@/shared/api";

type Step = "verify" | "items";

function readOptionDraft(productId: number): OrderItemInput[] | null {
  if (typeof window === "undefined") return null;
  try {
    const raw = sessionStorage.getItem("hg_guest_order_draft");
    if (!raw) return null;
    const draft = JSON.parse(raw) as {
      productId?: unknown;
      items?: Array<{
        productVariantId?: unknown;
        textInputs?: unknown;
        qty?: unknown;
      }>;
    };
    if (draft.productId !== productId || !Array.isArray(draft.items)) return null;
    const items = draft.items.flatMap((item): OrderItemInput[] => (
      Number.isSafeInteger(item.productVariantId)
      && Number.isSafeInteger(item.qty)
      && Number(item.qty) >= 1
      && Array.isArray(item.textInputs)
        ? [{
          productId,
          productVariantId: Number(item.productVariantId),
          textInputs: item.textInputs as OrderItemInput["textInputs"],
          qty: Number(item.qty),
        }]
        : []
    ));
    return items.length === draft.items.length ? items : null;
  } catch {
    return null;
  }
}

export function OrderCreatePage() {
  const { isLoading, sessionVersion } = useCustomerAuth();
  if (isLoading) {
    return <Container className="page-container"><LoadingSpinner /></Container>;
  }
  return <OrderCreateForm key={sessionVersion} />;
}

function OrderCreateForm() {
  const [searchParams] = useSearchParams();
  const { user } = useCustomerAuth();
  const [step, setStep] = useState<Step>(user ? "items" : "verify");
  const [manualEntryConfirmed, setManualEntryConfirmed] = useState(false);
  const [phone, setPhone] = useState("");
  const [code, setCode] = useState("");
  const [verificationReset, setVerificationReset] = useState({ version: 0, message: "" });
  const resetVerification = (message: string) => {
    setCode("");
    setVerificationReset((previous) => ({ version: previous.version + 1, message }));
  };
  const [name, setName] = useState(user?.name ?? "");
  const [nameTouched, setNameTouched] = useState(false);
  const [items, setItems] = useState<OrderItemInput[]>([]);
  const [selectedProductTypes, setSelectedProductTypes] = useState<ProductType[] | null>(null);
  const [itemAmount, setItemAmount] = useState(0);
  const [issuedCouponId, setIssuedCouponId] = useState<number | null>(null);
  const [rewardAmount, setRewardAmount] = useState(0);
  const [fulfillment, setFulfillment] = useFulfillmentSelection(
    user?.name ?? name,
    user?.phone ?? phone,
  );
  const normalizedName = name.trim();
  const productTypesReady = selectedProductTypes !== null;
  const requiresMadeToOrderConsent = selectedProductTypes?.includes("MADE_TO_ORDER") ?? false;
  const consent = useMadeToOrderConsent(requiresMadeToOrderConsent);
  const guestPolicyConsent = usePolicyAcceptance();
  const [checkoutSelection, setCheckoutSelection] = useCheckoutSelection();

  const prefilledProductId = Number(searchParams.get("productId"));
  const requestedQty = Number(searchParams.get("qty") ?? "1");
  const orderDraftType = searchParams.get("draft");
  const hasPrefilledItem = Number.isSafeInteger(prefilledProductId) && prefilledProductId > 0;
  const normalizedPrefilledQty = Number.isInteger(requestedQty) && requestedQty >= 1
    ? Math.min(requestedQty, MAX_PRODUCT_QUANTITY)
    : 1;
  const shouldShowManualEntryGate = !user && !hasPrefilledItem && !manualEntryConfirmed;
  const orderQuery = searchParams.toString();
  const loginHref = buildAuthPageHref("/login", {
    redirectTo: `/orders/new${orderQuery ? `?${orderQuery}` : ""}`,
  });

  useEffect(() => {
    setSelectedProductTypes(null);
    if (hasPrefilledItem) {
      const optionDraft = orderDraftType === "options"
        ? readOptionDraft(prefilledProductId)
        : null;
      setItems(optionDraft ?? [{
        productId: prefilledProductId,
        productVariantId: null,
        textInputs: [],
        qty: normalizedPrefilledQty,
      }]);
      setManualEntryConfirmed(true);
      return;
    }
    setItems([]);
  }, [hasPrefilledItem, normalizedPrefilledQty, orderDraftType, prefilledProductId]);

  useEffect(() => {
    if (!user) return;
    setStep("items");
    setName((current) => current || user.name);
  }, [user]);

  const mutation = useMutation({
    mutationFn: async () => {
      const payload: OrderPayload = user
        ? {
            type: "ORDER", userId: user.id, name: normalizedName || user.name, items,
            cartCheckout: false,
            madeToOrderConsent: consent.agreed,
            madeToOrderConsentVersion: consent.version,
            ...(issuedCouponId === null ? {} : { issuedCouponId }),
            rewardAmount,
            ...fulfillmentPayload(fulfillment),
          }
        : {
            type: "ORDER", phone, verificationCode: code, name: normalizedName, items,
            cartCheckout: false,
            madeToOrderConsent: consent.agreed,
            madeToOrderConsentVersion: consent.version,
            policyAcceptance: guestPolicyConsent.acceptance,
            ...fulfillmentPayload(fulfillment),
          };
      await executePaymentFlow({
        checkoutSelection,
        context: "ORDER",
        payload,
        onPrepared: !user ? () => resetVerification(
          "인증코드가 결제 준비에 사용되었습니다. 다시 결제하려면 새 인증코드를 받아 주세요.",
        ) : undefined,
        orderName: items.length === 1 && items[0]
          ? `상품 주문 (${items[0].qty}개)`
          : `상품 주문 ${items.length}건`,
        customerKey: user ? `member_${user.id}` : undefined,
        customerName: normalizedName,
        customerPhone: phone || undefined,
        returnHint: {
          customerName: normalizedName, customerPhone: phone,
          returnPath: `/orders/new${orderQuery ? `?${orderQuery}` : ""}`,
        },
      });
    },
    onError: (error) => {
      consent.handleSubmissionError(error);
      if (!user && error instanceof ApiError && error.code === "PHONE_VERIFICATION_FAILED") {
        resetVerification("인증코드가 올바르지 않거나 만료되었습니다. 새 인증코드를 받아 주세요.");
      }
    },
  });
  const consentVersionMismatch = isMadeToOrderConsentVersionMismatch(mutation.error);

  return (
    <Container className="page-container" style={{ maxWidth: 640 }}>
      <div className="legacy-order-banner mb-4">
        <Badge bg="light" text="dark" className="mb-2">비회원 주문</Badge>
        <h4 className="mb-2">비회원 주문</h4>
        <p className="text-muted-soft mb-3">
          회원 주문은 상품 상세에서 바로 진행하는 것이 기본 경로입니다.
          비회원 주문이나 여러 상품을 한 번에 주문할 때 이 화면에서 계속 진행할 수 있습니다.
        </p>
        <div className="d-flex flex-wrap gap-2">
          <LinkButton to="/products" variant="dark" size="sm">
            상품 보러가기
          </LinkButton>
          {!user && (
            <LinkButton to={loginHref} variant="outline-secondary" size="sm">
              로그인 후 주문하기
            </LinkButton>
          )}
        </div>
        {hasPrefilledItem && (
          <Alert variant="info" className="mt-3 mb-0">
            상품 상세에서 선택한 상품과 수량을 미리 담아두었습니다.
            필요하면 아래에서 다른 상품을 추가하거나 삭제할 수 있습니다.
          </Alert>
        )}
      </div>

      {shouldShowManualEntryGate ? (
        <Card className="mb-4 border-0 my-claim-card">
          <Card.Body className="p-4">
            <div className="legacy-order-step-label mb-2">권장 경로 확인</div>
            <h5 className="mb-2">주문할 상품을 먼저 선택해 주세요</h5>
            <p className="text-muted-soft mb-3">
              작품 목록에서 원하는 상품과 수량을 먼저 고르면 주문 정보를 자동으로 채워드립니다.
              여러 상품을 직접 선택해 주문하려면 아래에서 계속 진행할 수 있습니다.
            </p>
            <div className="guest-route-note mb-3">
              <div className="guest-route-note-title">주문 안내</div>
              <div className="small text-muted-soft">
                상품 선택이 아직 없다면 먼저 작품을 둘러보는 편이 간편합니다.
                계속 진행하면 비회원 다중 상품 주문을 수동으로 입력할 수 있습니다.
              </div>
            </div>
            <div className="d-flex flex-wrap gap-2">
              <LinkButton to="/products" variant="dark" size="sm">
                상품 먼저 고르기
              </LinkButton>
              <LinkButton
                to="/guest"
                state={{ monitoringSource: "order_manual_entry_gate" }}
                variant="outline-secondary"
                size="sm"
              >
                비회원 조회 안내
              </LinkButton>
              <Button
                variant="outline-primary"
                size="sm"
                onClick={() => {
                  trackClientEvent({
                    event: "GUEST_ORDER_DIRECT_ENTRY_CONTINUED",
                    path: "/orders/new",
                    source: "manual_entry_gate",
                    target: "order_items_step",
                  });
                  setManualEntryConfirmed(true);
                }}
              >
                비회원 다중 상품 주문 계속
              </Button>
            </div>
          </Card.Body>
        </Card>
      ) : !user ? (
        <Card className="mb-4">
          <Card.Body>
            <div className="legacy-order-step-label">1. 휴대폰 인증</div>
            {!code && verificationReset.message && <Alert variant="info">{verificationReset.message}</Alert>}
            <PhoneVerificationStep
              key={verificationReset.version}
              initialPhone={phone}
              confirming={mutation.isPending}
              purpose="GUEST_ORDER"
              onReset={() => setCode("")}
              onVerified={(p, c) => {
                setPhone(p);
                setCode(c);
                setStep("items");
              }}
            />
          </Card.Body>
        </Card>
      ) : null}

      {step === "items" && (
        <>
          <Card className="mb-4">
            <Card.Body>
              <div className="legacy-order-step-label">{user ? "1." : "2."} 주문자 정보</div>
              <Form.Group controlId="order-create-name">
                <Form.Label>주문자 이름</Form.Label>
                <Form.Control
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  onBlur={() => setNameTouched(true)}
                  placeholder="이름을 입력하세요"
                  isInvalid={nameTouched && !normalizedName}
                  aria-invalid={nameTouched && !normalizedName}
                  aria-describedby={
                    nameTouched && !normalizedName ? "order-create-name-error" : undefined
                  }
                />
                <Form.Control.Feedback id="order-create-name-error" type="invalid">
                  이름을 입력해 주세요.
                </Form.Control.Feedback>
              </Form.Group>
            </Card.Body>
          </Card>

          <Card className="mb-4">
            <Card.Header>{user ? "2." : "3."} 상품 선택</Card.Header>
            <Card.Body>
              <OrderItemsForm
                items={items}
                onChange={setItems}
                onItemAmountChange={setItemAmount}
                onProductTypesChange={setSelectedProductTypes}
              />
            </Card.Body>
          </Card>

          <Card className="mb-4">
            <Card.Header>{user ? "3." : "4."} 수령 방법</Card.Header>
            <Card.Body>
              <FulfillmentForm value={fulfillment} onChange={setFulfillment} />
            </Card.Body>
          </Card>

          <Card className="mb-4">
            <Card.Header>{user ? "4." : "5."} 결제 금액</Card.Header>
            <Card.Body>
              <OrderPriceSummary
                itemAmount={itemAmount}
                fulfillmentType={fulfillment.fulfillmentType}
              />
              {user && (
                <>
                  <hr />
                  <MemberOrderBenefits
                    productAmount={itemAmount}
                    selectedCouponId={issuedCouponId}
                    rewardPointsToUse={rewardAmount}
                    disabled={mutation.isPending}
                    onCouponChange={setIssuedCouponId}
                    onRewardPointsChange={setRewardAmount}
                  />
                </>
              )}
            </Card.Body>
          </Card>

          <PaymentMethodFields value={checkoutSelection} onChange={setCheckoutSelection} disabled={mutation.isPending} />
          <PaymentErrorAlert error={consentVersionMismatch ? null : mutation.error} />
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
          {!user && (
            <PolicyConsentFields
              id="guest-order-policy-consent"
              policy={guestPolicyConsent.policyQuery.data}
              checked={guestPolicyConsent.accepted}
              onChange={guestPolicyConsent.setAccepted}
              isLoading={guestPolicyConsent.policyQuery.isLoading}
              error={guestPolicyConsent.policyQuery.error}
            />
          )}

          <Button
            variant="primary" size="lg" className="w-100"
            disabled={!normalizedName || items.length === 0 || !productTypesReady
              || !isFulfillmentComplete(fulfillment) || !consent.ready
              || (!user && (!code || !guestPolicyConsent.ready)) || mutation.isPending}
            onClick={() => { if (!mutation.isPending) mutation.mutate(); }}>
            {mutation.isPending ? "결제창 여는 중..." : "결제 진행하기"}
          </Button>
        </>
      )}
    </Container>
  );
}
