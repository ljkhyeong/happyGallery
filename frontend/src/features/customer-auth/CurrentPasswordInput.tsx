import { Form } from "react-bootstrap";
import { PasswordLengthFeedback } from "@/shared/ui/PasswordLengthFeedback";
import { PASSWORD_MAX_UTF8_BYTES } from "@/shared/validation/password";

interface Props {
  value: string;
  onChange: (value: string) => void;
}

/** 본인 확인용 현재 비밀번호 칸과 길이 초과 안내. 라벨은 감싸는 Form.Group·화면이 정한다. */
export function CurrentPasswordInput({ value, onChange }: Props) {
  return (
    <>
      <Form.Control
        type="password"
        autoComplete="current-password"
        maxLength={PASSWORD_MAX_UTF8_BYTES}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        required
        autoFocus
      />
      <PasswordLengthFeedback value={value} />
    </>
  );
}
