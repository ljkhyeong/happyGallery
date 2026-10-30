import { Alert, Button } from "react-bootstrap";
import { ApiError, CustomerSessionChangedError } from "@/shared/api";
import { getUserMessage } from "@/shared/lib";
import type { ErrorCode } from "@/shared/types/error";

/** 실패가 아니라 운영 설정 상태를 알리는 오류 코드. 다시 시도해도 결과가 같다. */
const SETTING_STATE_CODES: ReadonlyArray<ErrorCode> = ["SMARTSTORE_DISABLED"];

/**
 * 연동 꺼짐처럼 설정 상태를 알리는 오류인지 확인한다. 같은 안내가 한 영역에 겹치지 않게 할 때도 쓴다.
 * false여도 다른 코드의 ApiError일 수 있어 타입 좁히기(error is ApiError) 대신 boolean을 돌려준다.
 */
export function isSettingStateError(error: unknown): boolean {
  return error instanceof ApiError && SETTING_STATE_CODES.some((code) => error.is(code));
}

interface Props {
  error: unknown;
  onRetry?: () => void;
  retrying?: boolean;
  retryLabel?: string;
  /** 바깥 간격. 기본은 아래 1rem이고, gap으로 간격을 정하는 결제 결과 카드 안에서는 "mb-0"을 넘긴다. */
  className?: string;
}

/**
 * 오류를 사용자 문구로 바꾼다. 서버 오류 코드 문구가 있으면 그 문구를 쓰고,
 * fallback을 넘기면 나머지 경우에 그 문구를, 넘기지 않으면 오류 종류별 기본 문구를 쓴다.
 */
export function describeError(error: unknown, fallback?: string): string {
  if (error instanceof ApiError) {
    return getUserMessage(error.code)
      ?? fallback
      ?? (error.status >= 500
        ? "서비스에 일시적인 문제가 발생했습니다. 잠시 후 다시 시도해 주세요."
        : "요청을 완료할 수 없습니다. 화면의 안내를 확인해 주세요.");
  }
  if (fallback !== undefined) return fallback;
  if (error instanceof Error && error.name === "AbortError") {
    return "요청 시간이 초과되었습니다. 네트워크 상태를 확인하고 다시 시도해 주세요.";
  }
  if (error instanceof TypeError && error.message === "Failed to fetch") {
    return "서비스에 연결할 수 없습니다. 네트워크 상태를 확인해 주세요.";
  }
  return "요청 중 문제가 발생했습니다. 잠시 후 다시 시도해 주세요.";
}

export function ErrorAlert({ error, onRetry, retrying = false, retryLabel = "다시 시도", className = "mb-3" }: Props) {
  if (!error || error instanceof CustomerSessionChangedError) return null;

  // 설정 상태는 오류 대신 회색 안내 상자로 보이고, 다시 시도해도 같으므로 버튼을 두지 않는다.
  if (isSettingStateError(error)) {
    return (
      <Alert variant="secondary" className={className} role="status">
        {describeError(error)}
      </Alert>
    );
  }

  return (
    <Alert variant="danger" className={className} role="alert">
      <div>{describeError(error)}</div>
      {onRetry && (
        <Button
          type="button"
          variant="outline-danger"
          size="sm"
          className="mt-2"
          disabled={retrying}
          onClick={onRetry}
        >
          {retrying ? "다시 확인 중..." : retryLabel}
        </Button>
      )}
    </Alert>
  );
}
