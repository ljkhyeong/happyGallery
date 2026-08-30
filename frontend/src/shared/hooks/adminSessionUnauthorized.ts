import { ApiError } from "@/shared/api";

export function isAdminSessionUnauthorized(error: unknown): error is ApiError {
  return error instanceof ApiError
    && error.status === 401
    && error.code === "UNAUTHORIZED";
}
