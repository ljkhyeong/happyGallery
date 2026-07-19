import { useMutation } from "@tanstack/react-query";
import { Button, Modal } from "react-bootstrap";
import { PhoneVerificationStep } from "@/features/booking-create/PhoneVerificationStep";
import { ErrorAlert, useToast } from "@/shared/ui";
import { registerInitialPhone } from "./phoneRegistrationApi";

interface Props {
  show: boolean;
  onClose: () => void;
  onRegistered: () => Promise<void>;
}

export function InitialPhoneRegistrationModal({ show, onClose, onRegistered }: Props) {
  const toast = useToast();
  const registration = useMutation({
    mutationFn: ({ phone, verificationCode }: { phone: string; verificationCode: string }) =>
      registerInitialPhone(phone, verificationCode),
    onSuccess: async () => {
      await onRegistered();
      toast.show("휴대폰 번호가 등록되었습니다.");
      onClose();
    },
  });

  return (
    <Modal show={show} onHide={onClose} centered>
      <Modal.Header closeButton>
        <Modal.Title className="fs-6">휴대폰 번호 등록</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <p className="text-muted-soft small mb-3">
          주문과 예약 안내를 받을 휴대폰 번호를 인증해 주세요.
        </p>
        <ErrorAlert error={registration.error} />
        <PhoneVerificationStep
          title="휴대폰 인증"
          description="인증된 번호는 연락과 비회원 이력 확인에 사용됩니다."
          confirmLabel="인증하고 등록"
          confirming={registration.isPending}
          onVerified={(phone, verificationCode) =>
            registration.mutate({ phone, verificationCode })
          }
        />
      </Modal.Body>
      <Modal.Footer>
        <Button variant="secondary" onClick={onClose} disabled={registration.isPending}>
          나중에
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
