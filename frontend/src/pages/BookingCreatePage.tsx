import { useEffect, useRef, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Container, Card, Form, Row, Col, Button } from "react-bootstrap";
import { useNavigate, useSearchParams } from "react-router-dom";
import { SlotSelectionStep } from "@/features/booking-create/SlotSelectionStep";
import { AuthGateModal } from "@/features/customer-auth/AuthGateModal";
import { useCustomerAuth, type CustomerUser } from "@/features/customer-auth/useCustomerAuth";
import { fetchMyPasses } from "@/features/my/api";
import { isPassAvailableForBooking } from "@/features/my/listUtils";
import {
  confirmPayment,
  executePaymentFlow,
  type BookingPayload,
} from "@/features/payment";
import { formatDateTime } from "@/shared/lib";
import { ErrorAlert, useToast } from "@/shared/ui";
import type { DepositPaymentMethod, PublicSlotResponse } from "@/shared/types";

type PaymentPath = "deposit" | "pass";

interface GuestInfo {
  phone: string;
  verificationCode: string;
  name: string;
}

interface PaymentActor {
  member?: CustomerUser;
  guest?: GuestInfo;
}

export function BookingCreatePage() {
  const toast = useToast();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { isAuthenticated, user } = useCustomerAuth();
  const passPrefillApplied = useRef(false);

  const [selectedSlot, setSelectedSlot] = useState<PublicSlotResponse | null>(null);
  const [paymentPath, setPaymentPath] = useState<PaymentPath>("deposit");
  const [paymentMethod, setPaymentMethod] = useState<DepositPaymentMethod>("CARD");
  const [passId, setPassId] = useState("");
  const [showGate, setShowGate] = useState(false);

  const { data: passes, isLoading: passesLoading, error: passesError } = useQuery({
    queryKey: ["my", "passes"],
    queryFn: fetchMyPasses,
    enabled: isAuthenticated,
  });
  const availablePasses = (passes ?? []).filter(isPassAvailableForBooking);
  const requestedPassId = Number(searchParams.get("passId"));
  const hasRequestedPass = Number.isSafeInteger(requestedPassId) && requestedPassId > 0;

  useEffect(() => {
    if (isAuthenticated) {
      return;
    }
    passPrefillApplied.current = false;
    if (paymentPath === "pass") {
      setPaymentPath("deposit");
      setPassId("");
    }
  }, [isAuthenticated, paymentPath]);

  useEffect(() => {
    if (!isAuthenticated || passes === undefined || passPrefillApplied.current) {
      return;
    }
    passPrefillApplied.current = true;
    if (hasRequestedPass && passes.some(
      (pass) => pass.passId === requestedPassId && isPassAvailableForBooking(pass),
    )) {
      setPaymentPath("pass");
      setPassId(String(requestedPassId));
    }
  }, [hasRequestedPass, isAuthenticated, passes, requestedPassId]);

  useEffect(() => {
    if (paymentPath === "pass" && passId && passes !== undefined
      && !passes.some((pass) => String(pass.passId) === passId && isPassAvailableForBooking(pass))) {
      setPassId("");
    }
  }, [passId, passes, paymentPath]);

  const parsedPassId = Number(passId);
  const passValid = isAuthenticated && paymentPath === "pass"
    ? Number.isSafeInteger(parsedPassId) && parsedPassId > 0
    : true;
  const formReady = selectedSlot !== null && passValid;

  const startPayment = useMutation({
    mutationFn: async (actor?: PaymentActor) => {
      const guest = actor?.guest;
      const member = actor?.member ?? user;
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
              userId: member!.id,
              slotId: selectedSlot!.id,
              passId: paymentPath === "pass" ? parsedPassId : undefined,
              paymentMethod: paymentPath === "pass" ? undefined : paymentMethod,
            };

      await executePaymentFlow({
        context: "BOOKING",
        payload,
        orderName: `예약 — ${selectedSlot!.startAt.slice(0, 16).replace("T", " ")}`,
        customerKey: member ? `member_${member.id}` : undefined,
        customerName: guest?.name ?? member?.name,
        customerPhone: guest?.phone ?? member?.phone ?? undefined,
        returnHint: {
          customerName: guest?.name ?? member?.name,
          customerPhone: guest?.phone ?? member?.phone ?? undefined,
        },
        onZeroAmount: async (prep) => {
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
        },
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
                    disabled={passesLoading || availablePasses.length === 0}
                    onChange={() => setPaymentPath("pass")}
                  />
                )}
              </div>
            </Form.Group>

            {isAuthenticated && <ErrorAlert error={passesError} />}

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
                <Form.Label>사용할 8회권</Form.Label>
                <Form.Select
                  value={passId}
                  onChange={(e) => setPassId(e.target.value)}
                  disabled={passesLoading || availablePasses.length === 0}
                >
                  <option value="">
                    {passesLoading
                      ? "8회권을 불러오는 중입니다"
                      : availablePasses.length === 0
                        ? "사용 가능한 8회권이 없습니다"
                        : "8회권을 선택하세요"}
                  </option>
                  {availablePasses.map((pass) => (
                    <option key={pass.passId} value={pass.passId}>
                      8회권 #{pass.passId} · 잔여 {pass.remainingCredits}회 · 만료 {formatDateTime(pass.expiresAt)}
                    </option>
                  ))}
                </Form.Select>
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
        onMemberConfirm={(member) => {
          setShowGate(false);
          startPayment.mutate({ member });
        }}
        onGuestConfirm={(info) => {
          setShowGate(false);
          startPayment.mutate({ guest: info });
        }}
      />
    </Container>
  );
}
