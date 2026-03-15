import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Container, Card, Form, Row, Col, Button } from "react-bootstrap";
import { SlotSelectionStep } from "@/features/booking-create/SlotSelectionStep";
import { BookingSuccessCard } from "@/features/booking-create/BookingSuccessCard";
import { AuthGateModal } from "@/features/customer-auth/AuthGateModal";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { createGuestBooking } from "@/features/booking-create/api";
import { api } from "@/shared/api";
import { ErrorAlert, useToast } from "@/shared/ui";
import { formatKRW } from "@/shared/lib";
import type { BookingResponse, DepositPaymentMethod, PublicSlotResponse } from "@/shared/types";

type PaymentPath = "deposit" | "pass";

interface MemberBookingResponse {
  bookingId: number;
  status: string;
  className: string;
  startAt: string;
  endAt: string;
  depositAmount: number;
}

export function BookingCreatePage() {
  const toast = useToast();
  const { isAuthenticated } = useCustomerAuth();

  const [selectedSlot, setSelectedSlot] = useState<PublicSlotResponse | null>(null);
  const [paymentPath, setPaymentPath] = useState<PaymentPath>("deposit");
  const [depositAmount, setDepositAmount] = useState("");
  const [paymentMethod, setPaymentMethod] = useState<DepositPaymentMethod>("CARD");
  const [passId, setPassId] = useState("");

  const [showGate, setShowGate] = useState(false);
  const [result, setResult] = useState<BookingResponse | null>(null);
  const [memberDone, setMemberDone] = useState(false);

  const guestMutation = useMutation({
    mutationFn: (guestInfo: { phone: string; verificationCode: string; name: string }) => {
      if (paymentPath === "pass") {
        return createGuestBooking({
          phone: guestInfo.phone,
          verificationCode: guestInfo.verificationCode,
          name: guestInfo.name,
          slotId: selectedSlot!.id,
          passId: Number(passId),
        });
      }
      return createGuestBooking({
        phone: guestInfo.phone,
        verificationCode: guestInfo.verificationCode,
        name: guestInfo.name,
        slotId: selectedSlot!.id,
        depositAmount: Number(depositAmount),
        paymentMethod,
      });
    },
    onSuccess: (booking) => {
      toast.show("예약이 완료되었습니다!");
      setResult(booking);
      setShowGate(false);
    },
  });

  const memberMutation = useMutation({
    mutationFn: () => {
      const body: Record<string, unknown> = { slotId: selectedSlot!.id };
      if (paymentPath === "pass") {
        body.passId = Number(passId);
      } else {
        body.depositAmount = Number(depositAmount);
        body.paymentMethod = paymentMethod;
      }
      return api<MemberBookingResponse>("/me/bookings", { method: "POST", body });
    },
    onSuccess: () => {
      toast.show("예약이 완료되었습니다!");
      setMemberDone(true);
      setShowGate(false);
    },
  });

  const depositValid = paymentPath === "deposit" ? Number(depositAmount) > 0 : true;
  const passValid = paymentPath === "pass" ? Number(passId) > 0 : true;
  const formReady = selectedSlot !== null && depositValid && passValid;

  if (result) {
    return (
      <Container className="page-container" style={{ maxWidth: 640 }}>
        <h4 className="mb-4">예약 완료</h4>
        <BookingSuccessCard booking={result} />
      </Container>
    );
  }

  if (memberDone) {
    return (
      <Container className="page-container" style={{ maxWidth: 640 }}>
        <h4 className="mb-4">예약 완료</h4>
        <div className="text-center">
          <p className="mb-3">예약이 완료되었습니다.</p>
          <Button as={"a" as any} href="/my" variant="primary">
            내 예약 확인하기
          </Button>
        </div>
      </Container>
    );
  }

  return (
    <Container className="page-container" style={{ maxWidth: 640 }}>
      <h4 className="mb-4">체험 예약</h4>

      {/* 1. 슬롯 선택 */}
      <Card className="mb-4">
        <Card.Body>
          <SlotSelectionStep
            selectedSlotId={selectedSlot?.id ?? null}
            onSelect={(slot) => setSelectedSlot(slot)}
            onDeselect={() => setSelectedSlot(null)}
          />
        </Card.Body>
      </Card>

      {/* 2. 결제 정보 */}
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
                <Form.Check
                  inline type="radio"
                  id="booking-path-pass" label="8회권 사용"
                  name="paymentPath"
                  checked={paymentPath === "pass"}
                  onChange={() => setPaymentPath("pass")}
                />
              </div>
            </Form.Group>

            {paymentPath === "deposit" ? (
              <Row className="g-2 mb-3">
                <Col xs={6}>
                  <Form.Group controlId="booking-deposit">
                    <Form.Label>예약금 (원)</Form.Label>
                    <Form.Control
                      type="number" min={1} value={depositAmount}
                      onChange={(e) => setDepositAmount(e.target.value)}
                      placeholder="30000"
                    />
                  </Form.Group>
                </Col>
                <Col xs={6}>
                  <Form.Group controlId="booking-method">
                    <Form.Label>결제 수단</Form.Label>
                    <Form.Select
                      value={paymentMethod}
                      onChange={(e) => setPaymentMethod(e.target.value as DepositPaymentMethod)}
                    >
                      <option value="CARD">카드</option>
                      <option value="EASY_PAY">간편결제</option>
                    </Form.Select>
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
              </Form.Group>
            )}
          </Card.Body>
        </Card>
      )}

      {/* 3. 예약 버튼 */}
      <ErrorAlert error={guestMutation.error ?? memberMutation.error} />

      <Button
        variant="primary" size="lg" className="w-100"
        disabled={!formReady || guestMutation.isPending || memberMutation.isPending}
        onClick={() => {
          if (isAuthenticated) {
            memberMutation.mutate();
          } else {
            setShowGate(true);
          }
        }}
      >
        {guestMutation.isPending || memberMutation.isPending
          ? "예약 처리 중..."
          : `예약하기${paymentPath === "deposit" && depositAmount ? ` (${formatKRW(Number(depositAmount))})` : ""}`}
      </Button>

      <AuthGateModal
        show={showGate}
        onClose={() => setShowGate(false)}
        onMemberConfirm={() => memberMutation.mutate()}
        onGuestConfirm={(info) => guestMutation.mutate(info)}
      />
    </Container>
  );
}
