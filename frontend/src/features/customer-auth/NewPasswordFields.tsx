import { Form } from "react-bootstrap";
import { PasswordLengthFeedback } from "@/shared/ui/PasswordLengthFeedback";
import { PASSWORD_MAX_UTF8_BYTES } from "@/shared/validation/password";

interface Props {
  /** 입력 칸 id 접두사. 확인 칸·도움말·오류 id도 이 값에서 만든다. */
  idPrefix: string;
  password: string;
  confirmation: string;
  onPasswordChange: (value: string) => void;
  onConfirmationChange: (value: string) => void;
  /** 비밀번호 변경처럼 현재 비밀번호와 비교할 때만 넘긴다. */
  sameAsCurrent?: boolean;
  confirmationClassName?: string;
}

/** 새 비밀번호와 확인 입력. 8자 이상 안내, 바이트 길이 경고, 불일치 오류를 같은 형태로 보여 준다. */
export function NewPasswordFields({
  idPrefix,
  password,
  confirmation,
  onPasswordChange,
  onConfirmationChange,
  sameAsCurrent,
  confirmationClassName,
}: Props) {
  const checksCurrent = sameAsCurrent !== undefined;
  const confirmationMismatch = confirmation.length > 0 && password !== confirmation;

  return (
    <>
      <Form.Group className="mb-3" controlId={idPrefix}>
        <Form.Label>새 비밀번호</Form.Label>
        <Form.Control
          type="password"
          autoComplete="new-password"
          value={password}
          onChange={(event) => onPasswordChange(event.target.value)}
          minLength={8}
          maxLength={PASSWORD_MAX_UTF8_BYTES}
          {...(checksCurrent ? { isInvalid: sameAsCurrent, "aria-invalid": sameAsCurrent } : {})}
          aria-describedby={sameAsCurrent ? `${idPrefix}-error` : `${idPrefix}-help`}
          required
        />
        <PasswordLengthFeedback value={password} />
        {checksCurrent && (
          <Form.Control.Feedback id={`${idPrefix}-error`} type="invalid">
            현재 비밀번호와 다른 값을 입력하세요.
          </Form.Control.Feedback>
        )}
        {!sameAsCurrent && (
          <Form.Text id={`${idPrefix}-help`} className="text-muted">
            8자 이상 입력하세요.
          </Form.Text>
        )}
      </Form.Group>
      <Form.Group className={confirmationClassName} controlId={`${idPrefix}-confirmation`}>
        <Form.Label>새 비밀번호 확인</Form.Label>
        <Form.Control
          type="password"
          autoComplete="new-password"
          value={confirmation}
          onChange={(event) => onConfirmationChange(event.target.value)}
          maxLength={PASSWORD_MAX_UTF8_BYTES}
          isInvalid={confirmationMismatch}
          aria-invalid={confirmationMismatch}
          aria-describedby={confirmationMismatch ? `${idPrefix}-confirmation-error` : undefined}
          required
        />
        <Form.Control.Feedback id={`${idPrefix}-confirmation-error`} type="invalid">
          새 비밀번호가 일치하지 않습니다.
        </Form.Control.Feedback>
      </Form.Group>
    </>
  );
}
