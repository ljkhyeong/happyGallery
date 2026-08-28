import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, Button, Form, Modal } from "react-bootstrap";
import type { VacancyAlertResponse } from "@/generated/api/booking";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import {
  captureCustomerSession,
  isCurrentCustomerSession,
  queryKeys,
  runForCustomerSession,
  runForCurrentCustomer,
  type CustomerSessionSnapshot,
} from "@/shared/api";
import { ErrorAlert, useToast } from "@/shared/ui";
import { PhoneVerificationStep } from "./PhoneVerificationStep";
import {
  cancelGuestVacancyAlert,
  cancelMyVacancyAlert,
  fetchMyVacancyAlerts,
  registerGuestVacancyAlert,
  registerMyVacancyAlert,
} from "./api";
import {
  clearGuestVacancyAlert,
  findGuestVacancyAlert,
  saveGuestVacancyAlert,
} from "./vacancyAlertSession";

interface Registration {
  owner: "guest" | "member";
  response: VacancyAlertResponse;
}

interface GuestRegistrationInput {
  name: string;
  phone: string;
  verificationCode: string;
}

interface RegistrationResult extends Registration {
  customerSession: CustomerSessionSnapshot;
}

export function VacancyAlertButton({ slotId }: { slotId: number }) {
  const {
    isAuthenticated,
    isLoading: authLoading,
    sessionVersion,
  } = useCustomerAuth();
  const queryClient = useQueryClient();
  const toast = useToast();
  const [showGuestForm, setShowGuestForm] = useState(false);
  const [guestName, setGuestName] = useState("");
  const [guestRegistration, setGuestRegistration] = useState(
    () => findGuestVacancyAlert(slotId),
  );
  const memberAlertsQuery = useQuery({
    queryKey: queryKeys.member.vacancyAlerts,
    queryFn: () => runForCurrentCustomer(fetchMyVacancyAlerts),
    enabled: isAuthenticated,
  });

  useEffect(() => {
    setGuestRegistration(
      isAuthenticated ? null : findGuestVacancyAlert(slotId),
    );
  }, [isAuthenticated, sessionVersion, slotId]);

  const memberRegistration = memberAlertsQuery.data
    ?.find((alert) => alert.slotId === slotId) ?? null;
  const registration: Registration | null = authLoading
    ? null
    : isAuthenticated
      ? memberRegistration && { owner: "member", response: memberRegistration }
      : guestRegistration && { owner: "guest", response: guestRegistration };

  const registerMutation = useMutation({
    mutationFn: (guest?: GuestRegistrationInput) => {
      const customerSession = captureCustomerSession();
      return runForCustomerSession(customerSession, async () => {
        if (isAuthenticated) {
          return {
            owner: "member",
            response: await registerMyVacancyAlert(slotId),
            customerSession,
          } satisfies RegistrationResult;
        }
        if (!guest) throw new Error("비회원 빈자리 알림 정보가 없습니다.");
        const response = await registerGuestVacancyAlert(slotId, guest);
        if (!response.accessToken) {
          throw new Error("비회원 빈자리 알림 취소 토큰을 받지 못했습니다.");
        }
        return {
          owner: "guest",
          response,
          customerSession,
        } satisfies RegistrationResult;
      });
    },
    onSuccess: (registered) => {
      if (!isCurrentCustomerSession(registered.customerSession)) return;
      if (registered.owner === "member") {
        queryClient.setQueryData<VacancyAlertResponse[]>(
          queryKeys.member.vacancyAlerts,
          (alerts = []) => [
            ...alerts.filter((alert) => alert.slotId !== slotId),
            registered.response,
          ],
        );
      } else if (saveGuestVacancyAlert(
        registered.response,
        registered.customerSession,
      )) {
        setGuestRegistration(registered.response);
      } else {
        return;
      }
      setShowGuestForm(false);
      toast.show("빈자리가 생기면 한 번 알려드릴게요.");
    },
  });

  const cancelMutation = useMutation({
    mutationFn: (target: Registration) => {
      const customerSession = captureCustomerSession();
      return runForCustomerSession(customerSession, async () => {
        if (target.owner === "member") {
          await cancelMyVacancyAlert(slotId);
        } else if (target.response.accessToken) {
          await cancelGuestVacancyAlert(slotId, target.response.accessToken);
        }
        return { ...target, customerSession } satisfies RegistrationResult;
      });
    },
    onSuccess: (canceled) => {
      if (!isCurrentCustomerSession(canceled.customerSession)) return;
      if (canceled.owner === "member") {
        queryClient.setQueryData<VacancyAlertResponse[]>(
          queryKeys.member.vacancyAlerts,
          (alerts = []) => alerts.filter((alert) => alert.slotId !== slotId),
        );
      } else if (clearGuestVacancyAlert(
        canceled.response,
        canceled.customerSession,
      )) {
        setGuestRegistration(null);
      } else {
        return;
      }
      toast.show("빈자리 알림 신청을 취소했습니다.", "info");
    },
  });

  if (registration) {
    return (
      <div>
        <Button
          type="button"
          size="sm"
          variant="outline-secondary"
          disabled={cancelMutation.isPending}
          onClick={() => cancelMutation.mutate(registration)}
        >
          {cancelMutation.isPending ? "취소 중..." : "빈자리 알림 취소"}
        </Button>
        <ErrorAlert error={cancelMutation.error} />
      </div>
    );
  }

  return (
    <>
      <div>
        <Button
          type="button"
          size="sm"
          variant="outline-primary"
          disabled={
            authLoading
            || memberAlertsQuery.isLoading
            || registerMutation.isPending
          }
          onClick={() => {
            registerMutation.reset();
            if (isAuthenticated) {
              registerMutation.mutate(undefined);
            } else {
              setShowGuestForm(true);
            }
          }}
        >
          {registerMutation.isPending ? "신청 중..." : "빈자리 알림"}
        </Button>
        {!showGuestForm && (
          <ErrorAlert error={memberAlertsQuery.error ?? registerMutation.error} />
        )}
      </div>

      <Modal
        show={showGuestForm}
        onHide={() => setShowGuestForm(false)}
        centered
      >
        <Modal.Header closeButton>
          <Modal.Title>빈자리 알림 신청</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Alert variant="info" className="small py-2">
            알림은 자리를 예약하지 않습니다. 안내를 받은 뒤 예약 화면에서 선착순으로 예약해 주세요.
          </Alert>
          <Form.Group controlId={`vacancy-alert-name-${slotId}`} className="mb-3">
            <Form.Label>이름</Form.Label>
            <Form.Control
              value={guestName}
              maxLength={100}
              onChange={(event) => setGuestName(event.target.value)}
              placeholder="알림 받을 분의 이름"
            />
          </Form.Group>
          <ErrorAlert error={registerMutation.error} />
          <PhoneVerificationStep
            purpose="GUEST_BOOKING"
            title="휴대폰 인증"
            description="빈자리 안내를 받을 휴대폰 번호를 확인합니다."
            confirmLabel="빈자리 알림 신청"
            confirming={registerMutation.isPending}
            confirmDisabled={!guestName.trim()}
            onVerified={(phone, verificationCode) => registerMutation.mutate({
              name: guestName.trim(),
              phone,
              verificationCode,
            })}
          />
        </Modal.Body>
      </Modal>
    </>
  );
}
