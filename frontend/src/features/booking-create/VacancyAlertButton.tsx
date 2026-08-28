import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Alert, Button, Form, Modal } from "react-bootstrap";
import type { VacancyAlertResponse } from "@/generated/api/booking";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { runForCurrentCustomer } from "@/shared/api";
import { ErrorAlert, useToast } from "@/shared/ui";
import { PhoneVerificationStep } from "./PhoneVerificationStep";
import {
  cancelGuestVacancyAlert,
  cancelMyVacancyAlert,
  registerGuestVacancyAlert,
  registerMyVacancyAlert,
} from "./api";

interface Registration {
  owner: "guest" | "member";
  response: VacancyAlertResponse;
}

interface GuestRegistrationInput {
  name: string;
  phone: string;
  verificationCode: string;
}

export function VacancyAlertButton({ slotId }: { slotId: number }) {
  const { isAuthenticated, isLoading: authLoading } = useCustomerAuth();
  const toast = useToast();
  const [showGuestForm, setShowGuestForm] = useState(false);
  const [guestName, setGuestName] = useState("");
  const [registration, setRegistration] = useState<Registration | null>(null);

  const registerMutation = useMutation({
    mutationFn: (guest?: GuestRegistrationInput) => runForCurrentCustomer(async () => {
      if (isAuthenticated) {
        return {
          owner: "member",
          response: await registerMyVacancyAlert(slotId),
        } satisfies Registration;
      }
      if (!guest) throw new Error("비회원 빈자리 알림 정보가 없습니다.");
      return {
        owner: "guest",
        response: await registerGuestVacancyAlert(slotId, guest),
      } satisfies Registration;
    }),
    onSuccess: (registered) => {
      setRegistration(registered);
      setShowGuestForm(false);
      toast.show("빈자리가 생기면 한 번 알려드릴게요.");
    },
  });

  const cancelMutation = useMutation({
    mutationFn: () => runForCurrentCustomer(async () => {
      if (!registration) return;
      if (registration.owner === "member") {
        await cancelMyVacancyAlert(slotId);
        return;
      }
      const accessToken = registration.response.accessToken;
      if (accessToken) await cancelGuestVacancyAlert(slotId, accessToken);
    }),
    onSuccess: () => {
      setRegistration(null);
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
          onClick={() => cancelMutation.mutate()}
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
          disabled={authLoading || registerMutation.isPending}
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
        {!showGuestForm && <ErrorAlert error={registerMutation.error} />}
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
