import { useEffect, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Button, Modal } from "react-bootstrap";
import { PhoneVerificationStep } from "@/features/booking-create/PhoneVerificationStep";
import {
  ApiError,
  runForCurrentCustomer,
} from "@/shared/api";
import { buildAuthPageHref } from "@/features/customer-auth/navigation";
import { CustomerStepUpPrompt } from "@/features/customer-auth/CustomerStepUpPrompt";
import { ErrorAlert, useToast } from "@/shared/ui";
import { updateMemberPhone } from "./phoneRegistrationApi";

interface Props {
  show: boolean;
  currentPhone: string | null;
  localPasswordEnabled: boolean;
  initiallyReauthenticated: boolean;
  onClose: () => void;
  onUpdated: () => Promise<void>;
}

export function MemberPhoneUpdateModal({
  show,
  currentPhone,
  localPasswordEnabled,
  initiallyReauthenticated,
  onClose,
  onUpdated,
}: Props) {
  const toast = useToast();
  const [stepUpBusy, setStepUpBusy] = useState(false);
  const [reauthenticated, setReauthenticated] = useState(
    currentPhone === null || initiallyReauthenticated,
  );

  useEffect(() => {
    if (show) {
      setReauthenticated(currentPhone === null || initiallyReauthenticated);
    }
  }, [show, currentPhone, initiallyReauthenticated]);

  const update = useMutation({
    mutationFn: ({ phone, verificationCode }: { phone: string; verificationCode: string }) =>
      runForCurrentCustomer(
        () => updateMemberPhone(phone, verificationCode),
        async (_, requireCurrent) => {
          toast.show(
            currentPhone ? "휴대폰 번호가 변경되었습니다." : "휴대폰 번호가 등록되었습니다.",
          );
          if (currentPhone) {
            window.location.assign(buildAuthPageHref("/login", { redirectTo: "/my" }));
            return;
          }
          await onUpdated();
          requireCurrent();
          onClose();
        },
      ),
    onError: (error) => {
      if (error instanceof ApiError && error.code === "REAUTHENTICATION_REQUIRED") {
        setReauthenticated(false);
      }
    },
  });

  function close() {
    if (update.isPending || stepUpBusy) return;
    update.reset();
    onClose();
  }

  const busy = update.isPending || stepUpBusy;

  return (
    <Modal
      show={show}
      aria-labelledby="member-phone-update-title"
      onHide={close}
      backdrop={busy ? "static" : true}
      keyboard={!busy}
      centered
    >
      <Modal.Header closeButton={!busy}>
        <Modal.Title id="member-phone-update-title" className="fs-6">
          휴대폰 번호 {currentPhone ? "변경" : "등록"}
        </Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <p className="text-muted-soft small mb-3">
          주문과 예약 안내를 받을 새 휴대폰 번호를 인증해 주세요.
        </p>
        <ErrorAlert error={reauthenticated ? update.error : null} />
        {!reauthenticated ? (
          <CustomerStepUpPrompt
            localPasswordEnabled={localPasswordEnabled}
            returnAction="phone-change"
            onVerified={() => setReauthenticated(true)}
            onBusyChange={setStepUpBusy}
          />
        ) : (
          <PhoneVerificationStep
            purpose={currentPhone ? "MEMBER_PHONE_CHANGE" : "MEMBER_PHONE_REGISTRATION"}
            key={`${show}-${currentPhone ?? "none"}`}
            title="휴대폰 인증"
            description="인증된 번호는 연락과 비회원 이력 확인에 사용됩니다."
            confirmLabel={currentPhone ? "인증하고 변경" : "인증하고 등록"}
            confirming={update.isPending}
            onVerified={(phone, verificationCode) => update.mutate({ phone, verificationCode })}
          />
        )}
      </Modal.Body>
      <Modal.Footer>
        <Button variant="secondary" onClick={close} disabled={busy}>
          {currentPhone ? "취소" : "나중에"}
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
