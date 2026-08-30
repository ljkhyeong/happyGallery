import { ApiError } from "@/shared/api";

export function isReviewContentChangedError(error: unknown): error is ApiError {
  return error instanceof ApiError
    && error.status === 409
    && error.code === "REVIEW_CONTENT_CHANGED";
}

export function isAdminReviewMutationConflict(error: unknown): error is ApiError {
  return error instanceof ApiError
    && error.status === 409
    && (error.code === "CONFLICT" || error.code === "REVIEW_CONTENT_CHANGED");
}
