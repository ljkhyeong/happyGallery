import { Form } from "react-bootstrap";
import { isPasswordWithinByteLimit } from "@/shared/validation/password";

export function PasswordLengthFeedback({ value }: { value: string }) {
  if (isPasswordWithinByteLimit(value)) return null;
  return (
    <Form.Text className="text-danger d-block" role="alert">
      비밀번호가 너무 깁니다. 더 짧게 입력해 주세요.
    </Form.Text>
  );
}
