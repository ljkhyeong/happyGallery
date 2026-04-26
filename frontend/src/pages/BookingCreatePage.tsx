import { useEffect, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Container, Card, Form, Row, Col, Button } from "react-bootstrap";
import { useNavigate } from "react-router-dom";
import { SlotSelectionStep } from "@/features/booking-create/SlotSelectionStep";
import { AuthGateModal } from "@/features/customer-auth/AuthGateModal";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import {
  confirmPayment,
  preparePayment,
  requestTossPayment,
  storePaymentReturnHint,
  type BookingPayload,
} from "@/features/payment";
import { ErrorAlert, useToast } from "@/shared/ui";
import type { DepositPaymentMethod, PublicSlotResponse } from "@/shared/types";

type PaymentPath = "deposit" | "pass";

interface GuestInfo {
  phone: string;
  verificationCode: string;
  name: string;
}

export function BookingCreatePage() {
  const toast = useToast();
  const navigate = useNavigate();
  const { isAuthenticated, user } = useCustomerAuth();

  const [selectedSlot, setSelectedSlot] = useState<PublicSlotResponse | null>(null);
  const [paymentPath, setPaymentPath] = useState<PaymentPath>("deposit");
  const [paymentMethod, setPaymentMethod] = useState<DepositPaymentMethod>("CARD");
  const [passId, setPassId] = useState("");
  const [showGate, setShowGate] = useState(false);

  useEffect(() => {
    if (!isAuthenticated && paymentPath === "pass") {
      setPaymentPath("deposit");
    }
  }, [isAuthenticated, paymentPath]);

  const passValid = isAuthenticated && paymentPath === "pass" ? Number(passId) > 0 : true;
  const formReady = selectedSlot !== null && passValid;

  const startPayment = useMutation({
    mutationFn: async (guest?: GuestInfo) => {
      const payload: BookingPayload =
        guest
          ? {
              type: "BOOKING",
              phone: guest.phone,
              verificationCode: guest.verificationCode,
              name: guest.name,
              slotId: selectedSlot!.id,
              paymentMethod,
            }
          : {
              type: "BOOKING",
              userId: user!.id,
              slotId: selectedSlot!.id,
              passId: paymentPath === "pass" ? Number(passId) : undefined,
              paymentMethod: paymentPath === "pass" ? undefined : paymentMethod,
            };

      const prep = await preparePayment("BOOKING", payload);

      // 8회권 사용 예약 — amount=0이면 PG 우회하고 바로 confirm
      if (prep.amount === 0) {
        const result = await confirmPayment({
          paymentKey: null,
          orderId: prep.orderId,
          amount: 0,
        });
        toast.show("예약이 완료되었습니다!");
        if (result.accessToken) {
          navigate("/guest/bookings", {
            state: { bookingId: result.domainId, token: result.accessToken },
          });
        } else {
          navigate(`/my/bookings/${result.domainId}`);
        }
        return;
      }

      storePaymentReturnHint({
        customerName: guest?.name ?? user?.name,
        customerPhone: guest?.phone ?? user?.phone,
      });
      await requestTossPayment({
        orderId: prep.orderId,
        amount: prep.amount,
        orderName: `예약 — ${selectedSlot!.startAt.slice(0, 16).replace("T", " ")}`,
        customerKey: user ? `member_${user.id}` : undefined,
        customerName: guest?.name ?? user?.name,
        customerMobilePhone: guest?.phone ?? user?.phone,
      });
    },
  });

  return (
    <Container className="page-container" style={{ maxWidth: 640 }}>
      <h4 className="mb-4">체험 예약</h4>

      <Card className="mb-4">
        <Card.Body>
          <SlotSelectionStep
            selectedSlotId={selectedSlot?.id ?? null}
            onSelect={(slot) => setSelectedSlot(slot)}
            onDeselect={() => setSelectedSlot(null)}
          />
        </Card.Body>
      </Card>

      {selectedSlot && (
        <Card className="mb-4">
          <Card.Body>
            <h6 className="mb-3">결제 방식</h6>

            <Form.Group className="mb-3">
              <div>
                <Form.Check
                  inline type="radio"
                  id="booking-path-deposit" label="예약금 결제"
                  name="paymentPath"
                  checked={paymentPath === "deposit"}
                  onChange={() => setPaymentPath("deposit")}
                />
                {isAuthenticated && (
                  <Form.Check
                    inline type="radio"
                    id="booking-path-pass" label="8회권 사용"
                    name="paymentPath"
                    checked={paymentPath === "pass"}
                    onChange={() => setPaymentPath("pass")}
                  />
                )}
              </div>
            </Form.Group>

            {paymentPath === "deposit" ? (
              <Row className="g-2 mb-3">
                <Col xs={12}>
                  <Form.Group controlId="booking-method">
                    <Form.Label>결제 수단</Form.Label>
                    <Form.Select
                      value={paymentMethod}
                      onChange={(e) => setPaymentMethod(e.target.value as DepositPaymentMethod)}
                    >
                      <option value="CARD">카드</option>
                      <option value="EASY_PAY">간편결제</option>
                    </Form.Select>
                    <Form.Text className="text-muted">
                      예약금은 클래스 가격의 10%로 자동 산출됩니다.
                    </Form.Text>
                  </Form.Group>
                </Col>
              </Row>
            ) : (
              <Form.Group controlId="booking-pass" className="mb-3">
                <Form.Label>8회권 ID</Form.Label>
                <Form.Control
                  type="number" min={1} value={passId}
                  onChange={(e) => setPassId(e.target.value)}
                  placeholder="8회권 ID"
                />
                <Form.Text className="text-muted">
                  잔여 횟수에서 1회가 차감되며 결제창은 열리지 않습니다.
                </Form.Text>
              </Form.Group>
            )}
            {!isAuthenticated && (
              <p className="text-muted-soft small mb-0">
                8회권 예약은 로그인한 회원만 이용할 수 있습니다.
              </p>
            )}
          </Card.Body>
        </Card>
      )}

      <ErrorAlert error={startPayment.error} />

      <Button
        variant="primary" size="lg" className="w-100"
        disabled={!formReady || startPayment.isPending}
        onClick={() => {
          if (isAuthenticated) {
            startPayment.mutate(undefined);
          } else {
            setShowGate(true);
          }
        }}
      >
        {startPayment.isPending
          ? paymentPath === "pass" ? "예약 처리 중..." : "결제창 여는 중..."
          : paymentPath === "pass" ? "8회권으로 예약하기" : "결제 진행하기"}
      </Button>

      <AuthGateModal
        show={showGate}
        onClose={() => setShowGate(false)}
        onMemberConfirm={() => {
          setShowGate(false);
          startPayment.mutate(undefined);
        }}
        onGuestConfirm={(info) => {
          setShowGate(false);
          startPayment.mutate(info);
        }}
      />
    </Container>
  );
}
