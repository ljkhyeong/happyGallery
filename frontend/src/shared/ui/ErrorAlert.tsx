import { Alert } from "react-bootstrap";
import { ApiError } from "@/shared/api";
import { getUserMessage } from "@/shared/lib";

interface Props {
  error: Error | null;
}

export function ErrorAlert({ error }: Props) {
  if (!error) return null;

  let message: string;

  if (error instanceof ApiError) {
    if (error.status >= 500) {
      message = "서버에 일시적인 문제가 발생했습니다. 잠시 후 다시 시도해 주세요.";
    } else {
      message = getUserMessage(error.code) ?? error.message;
    }
  } else if (error.name === "AbortError") {
    message = "요청 시간이 초과되었습니다. 네트워크 상태를 확인하고 다시 시도해 주세요.";
  } else if (error instanceof TypeError && error.message === "Failed to fetch") {
    message = "서버에 연결할 수 없습니다. 네트워크 상태를 확인해 주세요.";
  } else {
    message = "알 수 없는 오류가 발생했습니다.";
  }

  return (
    <Alert variant="danger" className="mb-3">
      {message}
    </Alert>
  );
}
