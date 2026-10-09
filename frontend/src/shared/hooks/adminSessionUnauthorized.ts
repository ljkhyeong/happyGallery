import { ApiError } from "@/shared/api";

export function isAdminSessionUnauthorized(error: unknown): error is ApiError {
  return error instanceof ApiError
    && error.status === 401
    && error.code === "UNAUTHORIZED";
}

/** 관리자 세션 만료(401)는 인증 화면으로 돌아가므로 화면 오류로 보이지 않는다. */
export function visibleAdminError<T>(error: T): T | null {
  return isAdminSessionUnauthorized(error) ? null : error;
}
