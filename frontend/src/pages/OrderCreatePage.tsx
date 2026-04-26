import { useEffect, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Container, Card, Button, Form, Badge, Alert } from "react-bootstrap";
import { Link, useSearchParams } from "react-router-dom";
import { PhoneVerificationStep } from "@/features/booking-create/PhoneVerificationStep";
import { trackClientEvent } from "@/features/monitoring/api";
import { OrderItemsForm } from "@/features/order/OrderItemsForm";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import {
  preparePayment,
  requestTossPayment,
  storePaymentReturnHint,
  type OrderPayload,
} from "@/features/payment";
import { ErrorAlert } from "@/shared/ui";
import type { OrderItemInput } from "@/shared/types";

type Step = "verify" | "items";
const MAX_QTY = 99;

export function OrderCreatePage() {
  const [searchParams] = useSearchParams();
  const { user } = useCustomerAuth();
  const [step, setStep] = useState<Step>(user ? "items" : "verify");
  const [manualEntryConfirmed, setManualEntryConfirmed] = useState(false);
  const [phone, setPhone] = useState("");
  const [code, setCode] = useState("");
  const [name, setName] = useState(user?.name ?? "");
  const [nameTouched, setNameTouched] = useState(false);
  const [items, setItems] = useState<OrderItemInput[]>([]);

  const prefilledProductId = Number(searchParams.get("productId"));
  const requestedQty = Number(searchParams.get("qty") ?? "1");
  const hasPrefilledItem = Number.isInteger(prefilledProductId) && prefilledProductId > 0;
  const normalizedPrefilledQty = Number.isInteger(requestedQty) && requestedQty >= 1
    ? Math.min(requestedQty, MAX_QTY)
    : 1;
  const shouldShowManualEntryGate = !user && !hasPrefilledItem && !manualEntryConfirmed;

  useEffect(() => {
    if (hasPrefilledItem) {
      setItems([{ productId: prefilledProductId, qty: normalizedPrefilledQty }]);
      setManualEntryConfirmed(true);
      return;
    }
    setItems([]);
  }, [hasPrefilledItem, normalizedPrefilledQty, prefilledProductId]);

  const mutation = useMutation({
    mutationFn: async () => {
      const payload: OrderPayload = user
        ? { type: "ORDER", userId: user.id, name: name || user.name, items }
        : { type: "ORDER", phone, verificationCode: code, name, items };
      const prep = await preparePayment("ORDER", payload);
      storePaymentReturnHint({ customerName: name, customerPhone: phone });
      await requestTossPayment({
        orderId: prep.orderId,
        amount: prep.amount,
        orderName: items.length === 1 && items[0]
          ? `상품 주문 (${items[0].qty}개)`
          : `상품 주문 ${items.length}건`,
        customerKey: user ? `member_${user.id}` : undefined,
        customerName: name,
        customerMobilePhone: phone || undefined,
      });
    },
  });

  return (
    <Container className="page-container" style={{ maxWidth: 640 }}>
      <div className="legacy-order-banner mb-4">
        <Badge bg="light" text="dark" className="mb-2">Legacy Guest Fallback</Badge>
        <h4 className="mb-2">비회원 주문</h4>
        <p className="text-muted-soft mb-3">
          회원 주문은 상품 상세에서 바로 진행하는 것이 기본 경로입니다.
          이 페이지는 비회원 주문이나 다중 상품 주문이 필요한 경우를 위한 fallback 화면으로 유지됩니다.
        </p>
        <div className="d-flex flex-wrap gap-2">
          <Button as={Link as any} to="/products" variant="dark" size="sm">
            상품 보러가기
          </Button>
          {!user && (
            <Button as={Link as any} to="/login" variant="outline-secondary" size="sm">
              로그인 후 주문하기
            </Button>
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
            <h5 className="mb-2">직접 진입한 비회원 주문은 보조 경로입니다</h5>
            <p className="text-muted-soft mb-3">
              일반적인 비회원 주문은 상품 상세에서 원하는 상품과 수량을 먼저 고른 뒤
              `/orders/new?productId=&qty=`로 내려오는 흐름을 권장합니다.
              이 화면은 다중 상품 수동 주문이나 운영 지원용 direct entry를 위해 유지합니다.
            </p>
            <div className="guest-route-note mb-3">
              <div className="guest-route-note-title">Fallback policy</div>
              <div className="small text-muted-soft">
                상품 선택이 아직 없다면 먼저 스토어를 둘러본 뒤 내려오는 편이 더 안전합니다.
                계속 진행하면 비회원 다중 상품 주문을 수동으로 입력할 수 있습니다.
              </div>
            </div>
            <div className="d-flex flex-wrap gap-2">
              <Button as={Link as any} to="/products" variant="dark" size="sm">
                상품 먼저 고르기
              </Button>
              <Button
                as={Link as any}
                to="/guest"
                state={{ monitoringSource: "order_manual_entry_gate" }}
                variant="outline-secondary"
                size="sm"
              >
                비회원 조회 안내
              </Button>
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
            <PhoneVerificationStep
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
                  isInvalid={nameTouched && !name.trim()}
                />
                <Form.Control.Feedback type="invalid">
                  이름을 입력해 주세요.
                </Form.Control.Feedback>
              </Form.Group>
            </Card.Body>
          </Card>

          <Card className="mb-4">
            <Card.Header>{user ? "2." : "3."} 상품 선택</Card.Header>
            <Card.Body>
              <OrderItemsForm items={items} onChange={setItems} />
            </Card.Body>
          </Card>

          <ErrorAlert error={mutation.error} />

          <Button
            variant="primary" size="lg" className="w-100"
            disabled={!name.trim() || items.length === 0 || mutation.isPending}
            onClick={() => { if (!mutation.isPending) mutation.mutate(); }}>
            {mutation.isPending ? "결제창 여는 중..." : "결제 진행하기"}
          </Button>
        </>
      )}
    </Container>
  );
}
