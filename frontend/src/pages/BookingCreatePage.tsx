import { useEffect, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, Container, Form, Button } from "react-bootstrap";
import { Link, useSearchParams } from "react-router";
import { splitBookingAmount } from "@/features/booking-create/bookingAmounts";
import { SlotSelectionStep } from "@/features/booking-create/SlotSelectionStep";
import { bookingCreateHref, classAcceptsPass, formatSlotStart } from "@/features/booking-create/upcomingSlots";
import { AuthGateModal, type GuestInfo } from "@/features/customer-auth/AuthGateModal";
import { useCustomerAuth, type CustomerUser } from "@/features/customer-auth/useCustomerAuth";
import { fetchAllMyPasses } from "@/features/my/api";
import { isPassAvailableForBooking } from "@/features/my/listUtils";
import {
  executePaymentFlow,
  PaymentMethodFields,
  PaymentErrorAlert,
  useCheckoutSelection,
} from "@/features/payment";
import type { BookingPayload } from "@/generated/api/payment";
import { ApiError, queryKeys, runForCurrentCustomer } from "@/shared/api";
import { classImageSrc, formatDateTime, formatKRW, getClassCategoryLabel, parsePositiveId } from "@/shared/lib";
import {
  CheckoutAmounts,
  CheckoutLayout,
  CheckoutPanel,
  CheckoutSummary,
  ErrorAlert,
  PageHeader,
} from "@/shared/ui";
import type { ClassResponse, PublicSlotResponse } from "@/generated/api/booking";
import type { DepositPaymentMethod } from "@/shared/types";

type PaymentPath = "deposit" | "pass";

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
  const queryClient = useQueryClient();
  const [searchParams] = useSearchParams();
  const { isAuthenticated, status: authStatus, user } = useCustomerAuth();
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
  const [guestVerificationReset, setGuestVerificationReset] = useState({ version: 0, message: "" });
  const resetGuestVerification = (message: string) => setGuestVerificationReset((previous) => ({
    version: previous.version + 1, message,
  }));

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
    queryFn: ({ signal }) => runForCurrentCustomer(() => fetchAllMyPasses(signal)),
    enabled: isAuthenticated,
  });
  const passAccepted = selectedClass !== null && classAcceptsPass(selectedClass);
  const availablePasses = useMemo(
    () => (passAccepted ? (passes ?? []).filter(isPassAvailableForBooking) : []),
    [passAccepted, passes],
  );
  const requestedPassId = parsePositiveId(searchParams.get("passId"));
  const hasRequestedPass = requestedPassId !== null;
  const requestedClassId = parsePositiveId(searchParams.get("classId"));
  const requestedSlotId = parsePositiveId(searchParams.get("slotId"));
  const selectRequestedSlot = searchParams.get("selectSlot") === "1";
  const initialClassId = selectedClass?.id ?? requestedClassId;
  const requestedPassCompatible = hasRequestedPass
    && availablePasses.some((pass) => pass.passId === requestedPassId);
  // 링크의 이용권을 고른 수업에 쓸 수 없다. 이용권을 받지 않는 수업이면 이용권 목록을 기다리지 않고 바로 안다.
  const requestedPassNeedsFallback = hasRequestedPass
    && isAuthenticated
    && selectedClass !== null
    && (!passAccepted || (passes !== undefined && !requestedPassCompatible));
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
  const { depositAmount, balanceAmount } = splitBookingAmount(totalAmount);

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
        onPrepared: guest ? () => resetGuestVerification(
          "이미 사용한 인증번호입니다. 다시 결제하려면 새 인증번호를 받아 주세요.",
        ) : undefined,
        // 결제창·영수증에 보이는 이름이라 화면과 같은 서울 시각 표기(예: 10/6(화) 오전 10:00)를 쓴다.
        orderName: `${selectedClass?.name ?? "클래스"} 예약 ${formatSlotStart(selectedSlot!)}`,
        customerKey: member ? `member_${member.id}` : undefined,
        customerName: guest?.name ?? member?.name,
        customerPhone: guest?.phone ?? member?.phone ?? undefined,
        returnHint: {
          customerName: guest?.name ?? member?.name,
          customerPhone: guest?.phone ?? member?.phone ?? undefined,
          // 결제를 마치지 못하고 돌아오면 고른 일정을 다시 선택해 둔다.
          returnPath: bookingCreateHref(selectedSlot!.classId, selectedSlot!.id, { selectSlot: true }),
        },
      });
    },
    onError: (error, actor) => {
      if (actor?.guest && error instanceof ApiError && error.code === "PHONE_VERIFICATION_FAILED") {
        resetGuestVerification("인증번호가 올바르지 않거나 만료되었습니다. 새 인증번호를 받아 주세요.");
      }
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

  const passPathSelected = paymentPath === "pass";
  // 이용권으로 예약하는 동안은 결제 수단을 숨기고 요약·버튼도 이용권 기준으로 보인다. 링크로 이용권을 고르고 들어와
  // 회원 확인이나 이용권 목록을 기다리는 동안도 같다. 그 수업에 쓸 수 없거나(fallback) 예약금 결제로 바꾸면 일반 예약이다.
  const passBooking = passPathSelected || (
    hasRequestedPass
    && (isAuthenticated || authStatus === "unknown")
    && !requestedPassNeedsFallback
    && !passFallbackAccepted
  );
  const submitButton = (
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
        ? passPathSelected ? "예약 처리 중..." : "결제창 여는 중..."
        : passBooking ? "이용권으로 예약하기" : "결제하기"}
    </Button>
  );

  return (
    <Container className="page-container">
      <PageHeader
        title={hasRequestedPass && passBooking ? "이용권으로 예약" : "체험 예약"}
        description={hasRequestedPass && passBooking
          ? "이용권을 쓸 수 있는 수업만 보여 드립니다. 예약하면 이용권 1회가 차감됩니다."
          : "수업과 시간을 고르고 예약금을 결제하면 자리가 확정됩니다."}
      />

      <CheckoutLayout
        summary={(
          <CheckoutSummary
            label="예약 요약"
            media={selectedClass ? <img src={classImageSrc(selectedClass)} alt="" /> : undefined}
            action={submitButton}
            // 모바일은 요약이 화면 아래에 있으므로 고정 바에 고른 일시를 함께 보여 준다.
            mobileTotal={selectedSlot && selectedClass
              ? passBooking
                ? { label: formatSlotStart(selectedSlot), amount: "이용권 1회" }
                : { label: formatSlotStart(selectedSlot), amount: `${formatKRW(depositAmount)} 결제` }
              : undefined}
            note={selectedSlot && !passBooking
              ? "결제 전에 예약 가능 여부와 최신 가격을 다시 확인합니다. 체험 전날 23:59까지 취소하면 예약금을 환불하며, 체험 당일 00:00부터는 환불되지 않습니다."
              : undefined}
          >
            {selectedClass ? (
              <>
                <h2 className="checkout-summary-title">{selectedClass.name}</h2>
                <p className="checkout-summary-meta">
                  {getClassCategoryLabel(selectedClass.category)} · {selectedClass.durationMin}분
                </p>
              </>
            ) : (
              <h2 className="checkout-summary-title">수업을 선택해 주세요</h2>
            )}
            <p className="checkout-summary-when">
              {selectedSlot
                ? `${formatSlotStart(selectedSlot)} · ${participantCount}명`
                : "날짜와 시간을 선택해 주세요"}
            </p>
            {selectedClass && selectedSlot && (passBooking ? (
              <CheckoutAmounts
                rows={[{ label: "1인 수업료", value: formatKRW(classPrice) }]}
                total={{ label: "이용권 차감", value: "1회" }}
              />
            ) : (
              <CheckoutAmounts
                rows={[
                  { label: "1인 수업료", value: formatKRW(classPrice) },
                  { label: `총 ${participantCount}명 금액`, value: formatKRW(totalAmount) },
                  { label: "체험 당일 현장 잔금", value: formatKRW(balanceAmount) },
                ]}
                total={{ label: "지금 결제할 예약금", value: formatKRW(depositAmount) }}
              />
            ))}
            <PaymentErrorAlert error={startPayment.error} />
            {matchingResume !== null && (
              <Alert variant="info" role="status">
                {isResumeRevalidating
                  ? "로그인이 완료되었습니다. 클래스와 일정의 최신 정보를 확인하고 있습니다."
                  : "로그인이 완료되었습니다. 선택한 클래스와 일정을 다시 확인한 뒤 결제를 계속해 주세요."}
              </Alert>
            )}
          </CheckoutSummary>
        )}
      >
        <SlotSelectionStep
          initialClassId={initialClassId}
          initialSlotId={requestedClassId !== null && initialClassId === requestedClassId
            ? requestedSlotId : null}
          selectInitialSlot={selectRequestedSlot}
          passOnly={hasRequestedPass}
          // 수업을 고르자마자 그 수업에 링크의 이용권을 쓸 수 없음을 알린다(시간을 고르기 전에도 보인다).
          classNotice={requestedPassNeedsFallback && !passFallbackAccepted && (
            <Alert variant="warning" className="mt-3 mb-0">
              <p className="mb-2">
                링크에서 선택한 이용권을 이 클래스에 사용할 수 없습니다.{" "}
                <Link to="/my/passes?status=ACTIVE">이용권 상태</Link>를 다시 확인하거나 예약금 결제로 계속할 수 있습니다.
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
          selectedSlot={selectedSlot}
          onSelect={(slot) => setSelectedSlot(slot)}
          onDeselect={() => setSelectedSlot(null)}
          onClassChange={setSelectedClass}
        />

        {selectedSlot && (
          <CheckoutPanel title="인원과 결제 방식">
            <Form.Group controlId="booking-participant-count" className="checkout-field-row">
              <div>
                <Form.Label>예약 인원</Form.Label>
                <Form.Text>
                  {passPathSelected
                    ? "이용권 예약은 본인 1명만 이용할 수 있습니다."
                    : `현재 최대 ${selectedSlot.remainingCapacity}명까지 예약할 수 있습니다.`}
                </Form.Text>
              </div>
              <Form.Control
                type="number" inputMode="numeric"
                min={1}
                max={selectedSlot.remainingCapacity}
                step={1}
                value={participantCount}
                disabled={passPathSelected}
                onChange={(event) => setParticipantCount(Number(event.target.value))}
              />
            </Form.Group>

            <div className="checkout-choices" role="radiogroup" aria-label="결제 방식">
              <Form.Check
                type="radio"
                id="booking-path-deposit"
                label={<>예약금 결제<small>지금 10%, 잔금은 체험 당일 현장에서</small></>}
                name="paymentPath"
                checked={paymentPath === "deposit"}
                onChange={() => {
                  setPaymentPath("deposit");
                  if (hasRequestedPass) setPassFallbackAccepted(true);
                }}
              />
              {isAuthenticated && (
                <Form.Check
                  type="radio"
                  id="booking-path-pass"
                  label={<>이용권 사용<small>잔여 횟수에서 1회 차감</small></>}
                  name="paymentPath"
                  checked={passPathSelected}
                  disabled={passesLoading || availablePasses.length === 0}
                  onChange={() => {
                    setPaymentPath("pass");
                    setParticipantCount(1);
                  }}
                />
              )}
            </div>

            {isAuthenticated && (
              <ErrorAlert
                error={passesError}
                onRetry={() => { void refetchPasses(); }}
                retrying={passesFetching}
              />
            )}

            {passPathSelected && (
              <Form.Group controlId="booking-pass" className="mt-3">
                <Form.Label>사용할 이용권</Form.Label>
                <Form.Select
                  value={passId}
                  onChange={(e) => setPassId(e.target.value)}
                  disabled={passesLoading || availablePasses.length === 0}
                >
                  <option value="">
                    {passesLoading
                      ? "이용권을 불러오는 중입니다"
                      : availablePasses.length === 0
                        ? "사용 가능한 이용권이 없습니다"
                        : "이용권을 선택하세요"}
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
              <p className="text-muted-soft small mt-3 mb-0">
                이용권 예약은 로그인한 회원만 이용할 수 있습니다.
              </p>
            )}
          </CheckoutPanel>
        )}

        {!passBooking && (
          <CheckoutPanel title="결제 수단">
            <PaymentMethodFields
              value={checkoutSelection}
              onChange={setCheckoutSelection}
              disabled={startPayment.isPending}
              showLegend={false}
            />
          </CheckoutPanel>
        )}
      </CheckoutLayout>

      <AuthGateModal
        show={showGate}
        guestVerificationReset={guestVerificationReset}
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
