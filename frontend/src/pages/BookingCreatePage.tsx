import { useEffect, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Container, Card, Form, Row, Col, Button } from "react-bootstrap";
import { useNavigate, useSearchParams } from "react-router";
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
import { invalidateSlotAvailability, queryKeys } from "@/shared/api";
import { formatDateTime, formatKRW } from "@/shared/lib";
import { ErrorAlert, useToast } from "@/shared/ui";
import type { ClassResponse, DepositPaymentMethod, PublicSlotResponse } from "@/shared/types";
import type { PolicyAcceptance } from "@/features/policy-consent/types";

type PaymentPath = "deposit" | "pass";

interface GuestInfo {
  phone: string;
  verificationCode: string;
  name: string;
  policyAcceptance: PolicyAcceptance;
}

interface PaymentActor {
  member?: CustomerUser;
  guest?: GuestInfo;
}

export function BookingCreatePage() {
  const toast = useToast();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [searchParams] = useSearchParams();
  const { isAuthenticated, user } = useCustomerAuth();
  const passPrefillApplied = useRef(false);

  const [selectedSlot, setSelectedSlot] = useState<PublicSlotResponse | null>(null);
  const [selectedClass, setSelectedClass] = useState<ClassResponse | null>(null);
  const [paymentPath, setPaymentPath] = useState<PaymentPath>("deposit");
  const [paymentMethod, setPaymentMethod] = useState<DepositPaymentMethod>("CARD");
  const [participantCount, setParticipantCount] = useState(1);
  const [passId, setPassId] = useState("");
  const [showGate, setShowGate] = useState(false);

  const { data: passes, isLoading: passesLoading, error: passesError } = useQuery({
    queryKey: queryKeys.member.passes,
    queryFn: fetchMyPasses,
    enabled: isAuthenticated,
  });
  const availablePasses = (passes ?? [])
    .filter(isPassAvailableForBooking)
    .filter((pass) => pass.planCode === "LEGACY_ALL_CLASSES"
      || (selectedClass?.passEligible === true && selectedClass.category !== "PERFUME"));
  const requestedPassId = Number(searchParams.get("passId"));
  const hasRequestedPass = Number.isSafeInteger(requestedPassId) && requestedPassId > 0;
  const requestedClassId = Number(searchParams.get("classId"));
  const initialClassId = Number.isSafeInteger(requestedClassId) && requestedClassId > 0
    ? requestedClassId
    : null;

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
    if (!isAuthenticated || passes === undefined || passPrefillApplied.current || !hasRequestedPass) {
      return;
    }
    const requestedPass = passes.find((pass) => pass.passId === requestedPassId);
    if (!requestedPass || !isPassAvailableForBooking(requestedPass)) {
      passPrefillApplied.current = true;
      return;
    }
    if (requestedPass.planCode !== "LEGACY_ALL_CLASSES" && selectedClass === null) {
      return;
    }

    passPrefillApplied.current = true;
    if (availablePasses.some((pass) => pass.passId === requestedPassId)) {
      setPaymentPath("pass");
      setPassId(String(requestedPassId));
    }
  }, [availablePasses, hasRequestedPass, isAuthenticated, passes, requestedPassId, selectedClass]);

  useEffect(() => {
    if (paymentPath === "pass" && passId && passes !== undefined
      && !availablePasses.some((pass) => String(pass.passId) === passId)) {
      setPassId("");
    }
  }, [availablePasses, passId, passes, paymentPath]);

  useEffect(() => {
    if (!selectedSlot) return;
    const maximumParticipants = Math.min(8, selectedSlot.remainingCapacity);
    if (participantCount > maximumParticipants) {
      setParticipantCount(1);
    }
  }, [participantCount, selectedSlot]);

  const parsedPassId = Number(passId);
  const passValid = isAuthenticated && paymentPath === "pass"
    ? Number.isSafeInteger(parsedPassId) && parsedPassId > 0
    : true;
  const participantCountValid = selectedSlot !== null
    && participantCount >= 1
    && participantCount <= Math.min(8, selectedSlot.remainingCapacity)
    && (paymentPath !== "pass" || participantCount === 1);
  const formReady = selectedSlot !== null && passValid && participantCountValid;
  const classPrice = selectedClass?.price ?? 0;
  const totalAmount = classPrice * participantCount;
  const depositAmount = Math.floor(totalAmount / 10);
  const balanceAmount = totalAmount - depositAmount;

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
              policyAcceptance: guest.policyAcceptance,
              slotId: selectedSlot!.id,
              participantCount,
              paymentMethod,
            }
          : {
              type: "BOOKING",
              userId: member!.id,
              slotId: selectedSlot!.id,
              participantCount,
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
          await Promise.all([
            queryClient.invalidateQueries({
              queryKey: queryKeys.member.bookings.all,
            }),
            queryClient.invalidateQueries({
              queryKey: queryKeys.member.passes,
            }),
            invalidateSlotAvailability(queryClient),
          ]);
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
            initialClassId={initialClassId}
            selectedSlotId={selectedSlot?.id ?? null}
            onSelect={(slot) => setSelectedSlot(slot)}
            onDeselect={() => setSelectedSlot(null)}
            onClassChange={setSelectedClass}
          />
        </Card.Body>
      </Card>

      {selectedSlot && (
        <Card className="mb-4">
          <Card.Body>
            <h6 className="mb-3">결제 방식</h6>

            <Form.Group controlId="booking-participant-count" className="mb-3">
              <Form.Label>예약 인원</Form.Label>
              <Form.Select
                value={participantCount}
                disabled={paymentPath === "pass"}
                onChange={(event) => setParticipantCount(Number(event.target.value))}
              >
                {Array.from(
                  { length: Math.min(8, selectedSlot.remainingCapacity) },
                  (_, index) => index + 1,
                ).map((count) => (
                  <option key={count} value={count}>{count}명</option>
                ))}
              </Form.Select>
              <Form.Text className="text-muted">
                {paymentPath === "pass"
                  ? "8회권 예약은 본인 1명만 이용할 수 있습니다."
                  : `현재 최대 ${Math.min(8, selectedSlot.remainingCapacity)}명까지 예약할 수 있습니다.`}
              </Form.Text>
            </Form.Group>

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
                    onChange={() => {
                      setPaymentPath("pass");
                      setParticipantCount(1);
                    }}
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
                      결제 직전 서버가 슬롯과 현재 가격을 다시 확인해 금액을 확정합니다.
                    </Form.Text>
                  </Form.Group>
                </Col>
                {selectedClass && (
                  <Col xs={12}>
                    <dl className="row small mb-0 mt-2">
                      <dt className="col-6 fw-normal text-muted">1인 클래스 금액</dt>
                      <dd className="col-6 text-end mb-2">{formatKRW(classPrice)}</dd>
                      <dt className="col-6 fw-normal text-muted">총 {participantCount}명 금액</dt>
                      <dd className="col-6 text-end mb-2">{formatKRW(totalAmount)}</dd>
                      <dt className="col-6 fw-normal text-muted">지금 결제할 예약금</dt>
                      <dd className="col-6 text-end mb-2 fw-semibold">{formatKRW(depositAmount)}</dd>
                      <dt className="col-6 fw-normal text-muted">체험 당일 현장 잔금</dt>
                      <dd className="col-6 text-end mb-0">{formatKRW(balanceAmount)}</dd>
                    </dl>
                    <p className="text-muted-soft small mt-3 mb-0">
                      체험일 00:00 전까지 취소하면 예약금을 환불하며, 이후에는 환불되지 않습니다.
                    </p>
                  </Col>
                )}
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
                      {pass.planName} #{pass.passId} · 잔여 {pass.remainingCredits}회 · 만료 {formatDateTime(pass.expiresAt)}
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
