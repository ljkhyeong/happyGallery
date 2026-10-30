import { useState, type FormEvent } from "react";
import { useMutation } from "@tanstack/react-query";
import { Button, Form, Modal } from "react-bootstrap";
import { ErrorAlert, useToast } from "@/shared/ui";
import { changePassword } from "./credentialApi";
import { isPasswordWithinByteLimit } from "@/shared/validation/password";
import { CurrentPasswordInput } from "./CurrentPasswordInput";
import { NewPasswordFields } from "./NewPasswordFields";
import { runForCurrentCustomer } from "@/shared/api";

interface Props {
  show: boolean;
  onClose: () => void;
  onChanged: () => Promise<void>;
}

export function PasswordChangeModal({ show, onClose, onChanged }: Props) {
  const toast = useToast();
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmation, setConfirmation] = useState("");

  const mutation = useMutation({
    mutationFn: () =>
      runForCurrentCustomer(
        () => changePassword(currentPassword, newPassword),
        async () => {
          toast.show("비밀번호가 변경되었습니다. 다시 로그인해 주세요.");
          clearAndClose();
          await onChanged();
        },
      ),
  });

  const passwordMatches = newPassword === confirmation;
  const sameAsCurrentPassword = newPassword.length >= 8 && currentPassword === newPassword;
  const canSubmit = currentPassword.length >= 8
    && newPassword.length >= 8
    && isPasswordWithinByteLimit(currentPassword)
    && isPasswordWithinByteLimit(newPassword)
    && passwordMatches
    && currentPassword !== newPassword;

  function close() {
    if (mutation.isPending) return;
    clearAndClose();
  }

  function clearAndClose() {
    setCurrentPassword("");
    setNewPassword("");
    setConfirmation("");
    mutation.reset();
    onClose();
  }

  function submit(event: FormEvent) {
    event.preventDefault();
    if (canSubmit) mutation.mutate();
  }

  return (
    <Modal
      show={show}
      aria-labelledby="password-change-title"
      onHide={close}
      backdrop={mutation.isPending ? "static" : true}
      keyboard={!mutation.isPending}
      centered
    >
      <Modal.Header closeButton={!mutation.isPending}>
        <Modal.Title id="password-change-title" className="fs-6">비밀번호 변경</Modal.Title>
      </Modal.Header>
      <Form onSubmit={submit} aria-busy={mutation.isPending}>
        <Modal.Body>
          <ErrorAlert error={mutation.error} />
          <Form.Group className="mb-3" controlId="current-password">
            <Form.Label>현재 비밀번호</Form.Label>
            <CurrentPasswordInput value={currentPassword} onChange={setCurrentPassword} />
          </Form.Group>
          <NewPasswordFields
            idPrefix="new-password"
            password={newPassword}
            confirmation={confirmation}
            onPasswordChange={setNewPassword}
            onConfirmationChange={setConfirmation}
            sameAsCurrent={sameAsCurrentPassword}
          />
        </Modal.Body>
        <Modal.Footer>
          <Button variant="outline-secondary" onClick={close} disabled={mutation.isPending}>
            닫기
          </Button>
          <Button type="submit" disabled={!canSubmit || mutation.isPending}>
            {mutation.isPending ? "변경 중..." : "비밀번호 변경"}
          </Button>
        </Modal.Footer>
      </Form>
    </Modal>
  );
}
