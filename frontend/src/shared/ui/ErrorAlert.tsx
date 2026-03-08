import { Alert } from "react-bootstrap";
import { ApiError } from "@/shared/api";

interface Props {
  error: Error | null;
}

export function ErrorAlert({ error }: Props) {
  if (!error) return null;

  const message =
    error instanceof ApiError
      ? error.message
      : "알 수 없는 오류가 발생했습니다.";

  return (
    <Alert variant="danger" className="mb-3">
      {message}
    </Alert>
  );
}
