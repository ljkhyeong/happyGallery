import { useState } from "react";
import { Button, Form } from "react-bootstrap";
import { ErrorAlert } from "@/shared/ui";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { changeAdminPassword } from "./api";
import { isPasswordWithinByteLimit } from "@/shared/validation/password";

interface Props {
  adminKey: string;
  onAuthError: () => void;
  onChanged: () => void;
}

export function AdminPasswordChangeForm({ adminKey, onAuthError, onChanged }: Props) {
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmation, setConfirmation] = useState("");
  const confirmationMismatch = confirmation.length > 0 && confirmation !== newPassword;
  const canSubmit = currentPassword.length > 0
    && newPassword.length >= 10
    && isPasswordWithinByteLimit(currentPassword)
    && isPasswordWithinByteLimit(newPassword)
    && confirmation === newPassword;

  const mutation = useAdminMutation(onAuthError, {
    mutationFn: () => changeAdminPassword(adminKey, { currentPassword, newPassword }),
    onSuccess: onChanged,
  });

  return (
    <Form
      onSubmit={(event) => {
        event.preventDefault();
        if (canSubmit && !mutation.isPending) mutation.mutate();
      }}
    >
      <div className="row g-3 align-items-end">
        <div className="col-md-4">
          <Form.Group controlId="admin-current-password">
            <Form.Label>현재 비밀번호</Form.Label>
            <Form.Control
              type="password"
              autoComplete="current-password"
              maxLength={72}
              value={currentPassword}
              disabled={mutation.isPending}
              onChange={(event) => setCurrentPassword(event.target.value)}
            />
          </Form.Group>
        </div>
        <div className="col-md-4">
          <Form.Group controlId="admin-new-password">
            <Form.Label>새 비밀번호</Form.Label>
            <Form.Control
              type="password"
              autoComplete="new-password"
              minLength={10}
              maxLength={72}
              value={newPassword}
              isInvalid={newPassword.length > 0 && newPassword.length < 10}
              disabled={mutation.isPending}
              onChange={(event) => setNewPassword(event.target.value)}
            />
            <Form.Control.Feedback type="invalid">
              새 비밀번호는 10자 이상이어야 합니다.
            </Form.Control.Feedback>
          </Form.Group>
        </div>
        <div className="col-md-4">
          <Form.Group controlId="admin-new-password-confirmation">
            <Form.Label>새 비밀번호 확인</Form.Label>
            <Form.Control
              type="password"
              autoComplete="new-password"
              maxLength={72}
              value={confirmation}
              isInvalid={confirmationMismatch}
              disabled={mutation.isPending}
              onChange={(event) => setConfirmation(event.target.value)}
            />
            <Form.Control.Feedback type="invalid">
              새 비밀번호가 일치하지 않습니다.
            </Form.Control.Feedback>
          </Form.Group>
        </div>
      </div>

      <ErrorAlert error={mutation.error} />
      <Button type="submit" variant="outline-dark" disabled={!canSubmit || mutation.isPending}>
        {mutation.isPending ? "변경 중..." : "비밀번호 변경"}
      </Button>
    </Form>
  );
}
