import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, Container, Card, Form, Row, Col, Button } from "react-bootstrap";
import { useNavigate, useSearchParams } from "react-router";
import { SlotSelectionStep } from "@/features/booking-create/SlotSelectionStep";
import { AuthGateModal } from "@/features/customer-auth/AuthGateModal";
import { useCustomerAuth, type CustomerUser } from "@/features/customer-auth/useCustomerAuth";
import { fetchAllMyPasses } from "@/features/my/api";
import { isPassAvailableForBooking } from "@/features/my/listUtils";
import {
  confirmPayment,
  executePaymentFlow,
  PaymentMethodFields,
  PaymentErrorAlert,
  useCheckoutSelection,
  type BookingPayload,
} from "@/features/payment";
import {
  captureCustomerSession,
  invalidateSlotAvailability,
  queryKeys,
} from "@/shared/api";
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
  guest?: GuestInfo;
}

interface BookingDraft {
  selectedSlot: PublicSlotResponse;
  selectedClass: ClassResponse | null;
  paymentPath: PaymentPath;
  participantCount: number;
  passId: string;
  passFallbackAccepted: boolean;
}

interface MemberBookingResume {
  boundMemberId: number | null;
  draft: BookingDraft;
}

export function BookingCreatePage() {
  const { sessionVersion, user } = useCustomerAuth();
  const [memberResume, setMemberResume] = useState<MemberBookingResume | null>(null);
  const boundMemberId = memberResume?.boundMemberId ?? null;

  useEffect(() => {
    if (boundMemberId === null || user === null || boundMemberId === user.id) {
      return;
    }
    setMemberResume((current) => {
      if (current?.boundMemberId === null || current?.boundMemberId === user.id) {
        return current;
      }
      return null;
    });
  }, [boundMemberId, user]);

  return (
    <BookingCreateContent
      key={`${sessionVersion}:${boundMemberId ?? "unbound"}`}
      memberResume={memberResume}
      onMemberAuthenticationStarted={(draft) => {
        setMemberResume({ boundMemberId: null, draft });
      }}
      onMemberAuthenticated={(member) => {
        setMemberResume((current) => {
          if (!current) return null;
          if (
            current.boundMemberId !== null
            && current.boundMemberId !== member.id
          ) {
            return null;
          }
          return { ...current, boundMemberId: member.id };
        });
      }}
      onMemberResumeHandled={() => setMemberResume(null)}
    />
  );
}

interface BookingCreateContentProps {
  memberResume: MemberBookingResume | null;
  onMemberAuthenticationStarted: (draft: BookingDraft) => void;
  onMemberAuthenticated: (member: CustomerUser) => void;
  onMemberResumeHandled: () => void;
}

function BookingCreateContent({
  memberResume,
  onMemberAuthenticationStarted,
  onMemberAuthenticated,
  onMemberResumeHandled,
}: BookingCreateContentProps) {
  const toast = useToast();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [searchParams] = useSearchParams();
  const { isAuthenticated, user } = useCustomerAuth();
  const matchingResume = memberResume !== null
    && memberResume.boundMemberId !== null
    && memberResume.boundMemberId === user?.id
    ? memberResume
    : null;
  const [isResumeRevalidating, setIsResumeRevalidating] = useState(
    () => matchingResume !== null,
  );

  const resumeDraft = matchingResume?.draft;
  const [selectedSlot, setSelectedSlot] = useState<PublicSlotResponse | null>(
    () => resumeDraft?.selectedSlot ?? null,
  );
  const [selectedClass, setSelectedClass] = useState<ClassResponse | null>(
    () => resumeDraft?.selectedClass ?? null,
  );
  const [paymentPath, setPaymentPath] = useState<PaymentPath>(
    () => resumeDraft?.paymentPath ?? "deposit",
  );
  const paymentMethod: DepositPaymentMethod = "CARD";
  const [checkoutSelection, setCheckoutSelection] = useCheckoutSelection();
  const [participantCount, setParticipantCount] = useState(
    () => resumeDraft?.participantCount ?? 1,
  );
  const [passId, setPassId] = useState(() => resumeDraft?.passId ?? "");
  const [passFallbackAccepted, setPassFallbackAccepted] = useState(
    () => resumeDraft?.passFallbackAccepted ?? false,
  );
  const [showGate, setShowGate] = useState(false);

  useEffect(() => {
    if (matchingResume === null) {
      setIsResumeRevalidating(false);
      return;
    }

    let current = true;
    setIsResumeRevalidating(true);
    void Promise.all([
      queryClient.invalidateQueries({ queryKey: queryKeys.catalog.classes }),
      queryClient.invalidateQueries({
        queryKey: queryKeys.slotAvailability.upcoming.all,
      }),
    ]).finally(() => {
      if (current) setIsResumeRevalidating(false);
    });
    return () => {
      current = false;
    };
  }, [matchingResume, queryClient]);

  const {
    data: passes,
    isLoading: passesLoading,
    isFetching: passesFetching,
    error: passesError,
    refetch: refetchPasses,
  } = useQuery({
    queryKey: queryKeys.member.passCandidates,
    queryFn: ({ signal }) => fetchAllMyPasses(signal),
    enabled: isAuthenticated,
  });
  const availablePasses = (passes ?? [])
    .filter(isPassAvailableForBooking)
    .filter((pass) => pass.planCode === "LEGACY_ALL_CLASSES"
      || (selectedClass?.passEligible === true && selectedClass.category !== "PERFUME"));
  const requestedPassId = Number(searchParams.get("passId"));
  const hasRequestedPass = Number.isSafeInteger(requestedPassId) && requestedPassId > 0;
  const requestedClassId = Number(searchParams.get("classId"));
  const initialClassId = selectedClass?.id
    ?? (Number.isSafeInteger(requestedClassId) && requestedClassId > 0
      ? requestedClassId
      : null);
  const requestedPassCompatible = hasRequestedPass
    && availablePasses.some((pass) => pass.passId === requestedPassId);
  const requestedPassNeedsFallback = hasRequestedPass
    && isAuthenticated
    && passes !== undefined
    && selectedClass !== null
    && !requestedPassCompatible;
  const requestedPassPrefillPending = hasRequestedPass
    && isAuthenticated
    && requestedPassCompatible
    && paymentPath !== "pass";
  const requestedPassIntentBlocked = hasRequestedPass
    && isAuthenticated
    && !passFallbackAccepted
    && (
      passes === undefined
      || requestedPassNeedsFallback
      || requestedPassPrefillPending
    );

  useEffect(() => {
    if (isAuthenticated) {
      return;
    }
    setPassFallbackAccepted(false);
    if (paymentPath === "pass") {
      setPaymentPath("deposit");
      setPassId("");
    }
  }, [isAuthenticated, paymentPath]);

  useEffect(() => {
    if (!isAuthenticated || passes === undefined || !requestedPassCompatible) {
      return;
    }
    setPaymentPath("pass");
    setPassId(String(requestedPassId));
    setPassFallbackAccepted(false);
  }, [
    isAuthenticated,
    passes,
    requestedPassCompatible,
    requestedPassId,
    selectedClass?.id,
  ]);

  useEffect(() => {
    setPassFallbackAccepted(false);
  }, [requestedPassId, selectedClass?.id]);

  useEffect(() => {
    if (paymentPath === "pass" && passId && passes !== undefined
      && !availablePasses.some((pass) => String(pass.passId) === passId)) {
      setPassId("");
    }
  }, [availablePasses, passId, passes, paymentPath]);

  useEffect(() => {
    if (!selectedSlot) return;
    const maximumParticipants = selectedSlot.remainingCapacity;
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
    && participantCount <= selectedSlot.remainingCapacity
    && (paymentPath !== "pass" || participantCount === 1);
  const formReady = selectedSlot !== null
    && passValid
    && participantCountValid
    && !requestedPassIntentBlocked;
  const classPrice = selectedClass?.price ?? 0;
  const totalAmount = classPrice * participantCount;
  const depositAmount = Math.floor(totalAmount / 10);
  const balanceAmount = totalAmount - depositAmount;

  const startPayment = useMutation({
    mutationFn: async (actor?: PaymentActor) => {
      const guest = actor?.guest;
      const member = user;
      if (!guest && !member) {
        throw new Error("로그인 상태를 다시 확인해 주세요.");
      }
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
        checkoutSelection: paymentPath === "deposit" ? checkoutSelection : undefined,
        context: "BOOKING",
        payload,
        orderName: `예약 — ${selectedSlot!.startAt.slice(0, 16).replace("T", " ")}`,
        customerKey: member ? `member_${member.id}` : undefined,
        customerName: guest?.name ?? member?.name,
        customerPhone: guest?.phone ?? member?.phone ?? undefined,
        returnHint: {
          customerName: guest?.name ?? member?.name,
          customerPhone: guest?.phone ?? member?.phone ?? undefined,
          returnPath: `/bookings/new?classId=${selectedSlot!.classId}`,
        },
        onZeroAmount: async (prep, requireCurrentCustomer) => {
          const result = await confirmPayment({
            paymentKey: null,
            orderId: prep.orderId,
            amount: 0,
          });
          requireCurrentCustomer();
          await Promise.all([
            queryClient.invalidateQueries({
              queryKey: queryKeys.member.bookings.all,
            }),
            queryClient.invalidateQueries({
              queryKey: queryKeys.member.passes,
            }),
            invalidateSlotAvailability(queryClient),
          ]);
          requireCurrentCustomer();
          toast.show("예약이 완료되었습니다!");
          if (result.accessToken) {
            const customerSession = captureCustomerSession();
            requireCurrentCustomer();
            navigate("/guest/bookings", {
              state: {
                bookingId: result.domainId,
                token: result.accessToken,
                customerSession,
              },
            });
          } else {
            navigate(`/my/bookings/${result.domainId}`);
          }
        },
      });
    },
  });

  const captureBookingDraft = (): BookingDraft | null => selectedSlot
    ? {
        selectedSlot,
        selectedClass,
        paymentPath,
        participantCount,
        passId,
        passFallbackAccepted,
      }
    : null;

  return (
    <Container className="page-container" style={{ maxWidth: 640 }}>
      <h4 className="mb-4">체험 예약</h4>

      <Card className="mb-4">
        <Card.Body>
          <SlotSelectionStep
            initialClassId={initialClassId}
            selectedSlot={selectedSlot}
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
              <Form.Control
                type="number"
                min={1}
                max={selectedSlot.remainingCapacity}
                step={1}
                value={participantCount}
                disabled={paymentPath === "pass"}
                onChange={(event) => setParticipantCount(Number(event.target.value))}
              />
              <Form.Text className="text-muted">
                {paymentPath === "pass"
                  ? "8회권 예약은 본인 1명만 이용할 수 있습니다."
                  : `현재 최대 ${selectedSlot.remainingCapacity}명까지 예약할 수 있습니다.`}
              </Form.Text>
            </Form.Group>

            <Form.Group className="mb-3">
              <div>
                <Form.Check
                  inline type="radio"
                  id="booking-path-deposit" label="예약금 결제"
                  name="paymentPath"
                  checked={paymentPath === "deposit"}
                  onChange={() => {
                    setPaymentPath("deposit");
                    if (hasRequestedPass) setPassFallbackAccepted(true);
                  }}
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

            {isAuthenticated && (
              <ErrorAlert
                error={passesError}
                onRetry={() => { void refetchPasses(); }}
                retrying={passesFetching}
              />
            )}

            {requestedPassNeedsFallback && !passFallbackAccepted && (
              <Alert variant="warning">
                <p className="mb-2">
                  링크에서 선택한 8회권을 이 클래스에 사용할 수 없습니다.
                  이용권 상태를 다시 확인하거나 예약금 결제로 계속할 수 있습니다.
                </p>
                <Button
                  type="button"
                  variant="outline-dark"
                  size="sm"
                  onClick={() => {
                    setPaymentPath("deposit");
                    setPassId("");
                    setPassFallbackAccepted(true);
                  }}
                >
                  예약금 결제로 계속
                </Button>
              </Alert>
            )}

            {paymentPath === "deposit" ? (
              <Row className="g-2 mb-3">
                <Col xs={12}>
                  <p className="small text-muted mb-0">
                    결제 전에 예약 가능 여부와 최신 가격을 다시 확인합니다.
                  </p>
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
                      체험 전날 23:59까지 취소하면 예약금을 환불하며, 체험 당일 00:00부터는 환불되지 않습니다.
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

      {paymentPath === "deposit" && (
        <PaymentMethodFields value={checkoutSelection} onChange={setCheckoutSelection} disabled={startPayment.isPending} />
      )}
      <PaymentErrorAlert error={startPayment.error} />

      {matchingResume !== null && (
        <Alert variant="info" role="status">
          {isResumeRevalidating
            ? "로그인이 완료되었습니다. 클래스와 일정의 최신 정보를 확인하고 있습니다."
            : "로그인이 완료되었습니다. 선택한 클래스와 일정을 다시 확인한 뒤 결제를 계속해 주세요."}
        </Alert>
      )}

      <Button
        variant="primary" size="lg" className="w-100"
        disabled={!formReady || isResumeRevalidating || startPayment.isPending}
        onClick={() => {
          if (isAuthenticated) {
            onMemberResumeHandled();
            startPayment.mutate(undefined);
          } else {
            const draft = captureBookingDraft();
            if (!draft) return;
            onMemberAuthenticationStarted(draft);
            setShowGate(true);
          }
        }}
      >
        {isResumeRevalidating
          ? "선택 내용 확인 중..."
          : startPayment.isPending
          ? paymentPath === "pass" ? "예약 처리 중..." : "결제창 여는 중..."
          : paymentPath === "pass" ? "8회권으로 예약하기" : "결제 진행하기"}
      </Button>

      <AuthGateModal
        show={showGate}
        onClose={() => {
          onMemberResumeHandled();
          setShowGate(false);
        }}
        onMemberAuthenticated={(member: CustomerUser) => {
          onMemberAuthenticated(member);
        }}
        onGuestConfirm={(info) => {
          onMemberResumeHandled();
          setShowGate(false);
          startPayment.mutate({ guest: info });
        }}
      />
    </Container>
  );
}
