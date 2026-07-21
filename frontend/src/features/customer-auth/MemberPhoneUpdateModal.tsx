import { useMutation } from "@tanstack/react-query";
import { Button, Modal } from "react-bootstrap";
import { PhoneVerificationStep } from "@/features/booking-create/PhoneVerificationStep";
import { ErrorAlert, useToast } from "@/shared/ui";
import { updateMemberPhone } from "./phoneRegistrationApi";

interface Props {
  show: boolean;
  currentPhone: string | null;
  onClose: () => void;
  onUpdated: () => Promise<void>;
}

export function MemberPhoneUpdateModal({ show, currentPhone, onClose, onUpdated }: Props) {
  const toast = useToast();
  const update = useMutation({
    mutationFn: ({ phone, verificationCode }: { phone: string; verificationCode: string }) =>
      updateMemberPhone(phone, verificationCode),
    onSuccess: async () => {
      await onUpdated();
      toast.show(currentPhone ? "휴대폰 번호가 변경되었습니다." : "휴대폰 번호가 등록되었습니다.");
      close();
    },
  });

  function close() {
    if (update.isPending) return;
    update.reset();
    onClose();
  }

  return (
    <Modal show={show} onHide={close} centered>
      <Modal.Header closeButton>
        <Modal.Title className="fs-6">휴대폰 번호 {currentPhone ? "변경" : "등록"}</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <p className="text-muted-soft small mb-3">
          주문과 예약 안내를 받을 새 휴대폰 번호를 인증해 주세요.
        </p>
        <ErrorAlert error={update.error} />
        <PhoneVerificationStep
          key={`${show}-${currentPhone ?? "none"}`}
          title="휴대폰 인증"
          description="인증된 번호는 연락과 비회원 이력 확인에 사용됩니다."
          confirmLabel={currentPhone ? "인증하고 변경" : "인증하고 등록"}
          confirming={update.isPending}
          onVerified={(phone, verificationCode) => update.mutate({ phone, verificationCode })}
        />
      </Modal.Body>
      <Modal.Footer>
        <Button variant="secondary" onClick={close} disabled={update.isPending}>
          {currentPhone ? "취소" : "나중에"}
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
