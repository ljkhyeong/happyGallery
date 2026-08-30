import { useState, type FormEvent } from "react";
import { useMutation } from "@tanstack/react-query";
import { Button, Form, Modal } from "react-bootstrap";
import { ErrorAlert, useToast } from "@/shared/ui";
import { changePassword } from "./credentialApi";
import { isPasswordWithinByteLimit } from "@/shared/validation/password";
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
  const confirmationMismatch = confirmation.length > 0 && !passwordMatches;
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
            <Form.Control
              type="password"
              autoComplete="current-password"
              value={currentPassword}
              onChange={(event) => setCurrentPassword(event.target.value)}
              minLength={8}
              maxLength={72}
              required
              autoFocus
            />
          </Form.Group>
          <Form.Group className="mb-3" controlId="new-password">
            <Form.Label>새 비밀번호</Form.Label>
            <Form.Control
              type="password"
              autoComplete="new-password"
              value={newPassword}
              onChange={(event) => setNewPassword(event.target.value)}
              minLength={8}
              maxLength={72}
              isInvalid={sameAsCurrentPassword}
              aria-invalid={sameAsCurrentPassword}
              aria-describedby={
                sameAsCurrentPassword ? "new-password-error" : "new-password-help"
              }
              required
            />
            <Form.Control.Feedback id="new-password-error" type="invalid">
              현재 비밀번호와 다른 값을 입력하세요.
            </Form.Control.Feedback>
            {!sameAsCurrentPassword && (
              <Form.Text id="new-password-help" className="text-muted">
                8자 이상, UTF-8 기준 72바이트 이하로 입력하세요.
              </Form.Text>
            )}
          </Form.Group>
          <Form.Group controlId="new-password-confirmation">
            <Form.Label>새 비밀번호 확인</Form.Label>
            <Form.Control
              type="password"
              autoComplete="new-password"
              value={confirmation}
              onChange={(event) => setConfirmation(event.target.value)}
              maxLength={72}
              isInvalid={confirmationMismatch}
              aria-invalid={confirmationMismatch}
              aria-describedby={
                confirmationMismatch ? "new-password-confirmation-error" : undefined
              }
              required
            />
            <Form.Control.Feedback id="new-password-confirmation-error" type="invalid">
              새 비밀번호가 일치하지 않습니다.
            </Form.Control.Feedback>
          </Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="outline-secondary" onClick={close} disabled={mutation.isPending}>
            취소
          </Button>
          <Button type="submit" disabled={!canSubmit || mutation.isPending}>
            {mutation.isPending ? "변경 중..." : "비밀번호 변경"}
          </Button>
        </Modal.Footer>
      </Form>
    </Modal>
  );
}
