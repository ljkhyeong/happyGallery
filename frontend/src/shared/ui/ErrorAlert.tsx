import { Alert } from "react-bootstrap";
import { ApiError, CustomerSessionChangedError } from "@/shared/api";
import { getUserMessage } from "@/shared/lib";

interface Props {
  error: unknown;
}

export function ErrorAlert({ error }: Props) {
  if (!error || error instanceof CustomerSessionChangedError) return null;

  let message: string;

  if (error instanceof ApiError) {
    message = getUserMessage(error.code)
      ?? (error.status >= 500
        ? "서버에 일시적인 문제가 발생했습니다. 잠시 후 다시 시도해 주세요."
        : error.message);
  } else if (error instanceof Error && error.name === "AbortError") {
    message = "요청 시간이 초과되었습니다. 네트워크 상태를 확인하고 다시 시도해 주세요.";
  } else if (error instanceof TypeError && error.message === "Failed to fetch") {
    message = "서버에 연결할 수 없습니다. 네트워크 상태를 확인해 주세요.";
  } else {
    message = "알 수 없는 오류가 발생했습니다.";
  }

  return (
    <Alert variant="danger" className="mb-3" role="alert">
      {message}
    </Alert>
  );
}
