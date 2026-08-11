import {
  decideAdminReviewReport,
  deleteOfficialReviewReply,
  getAdminReview,
  getAdminReviewReport,
  getAdminReviewEvidenceImage,
  getAdminReviewImage,
  listAdminReviewReports,
  listAdminReviews,
  listReviewModerationActions,
  upsertOfficialReviewReply,
  updateAdminReviewStatus,
  type AdminReviewReportPageResponse,
  type AdminReviewReportResponse,
  type AdminReviewReportSummaryResponse,
  type AdminReviewPageResponse,
  type AdminReviewResponse,
  type DecideReviewReportRequest,
  type ListAdminReviewReportsParams,
  type ListAdminReviewsStatus,
  type ListAdminReviewsTargetType,
  type ReviewModerationActionResponse,
  type UpdateReviewStatusRequestStatus,
} from "@/generated/api/review";
import { adminHeaders } from "@/shared/api";

export type ReviewReportStatus = NonNullable<ListAdminReviewReportsParams["status"]>;
export type ReviewReportDecision = DecideReviewReportRequest["decision"];

export type {
  AdminReviewReportResponse,
  AdminReviewReportSummaryResponse,
  AdminReviewResponse,
  ListAdminReviewsStatus,
  ListAdminReviewsTargetType,
  ReviewModerationActionResponse,
};

export function fetchAdminReviews(
  adminKey: string,
  filters: {
    targetType?: ListAdminReviewsTargetType;
    status?: ListAdminReviewsStatus;
    cursor?: string;
  },
  signal?: AbortSignal,
): Promise<AdminReviewPageResponse> {
  return listAdminReviews(
    { ...filters, size: 20 },
    { headers: adminHeaders(adminKey), signal },
  );
}

export function fetchAdminReview(
  adminKey: string,
  reviewId: number,
  signal?: AbortSignal,
): Promise<AdminReviewResponse> {
  return getAdminReview(
    reviewId,
    { headers: adminHeaders(adminKey), signal },
  );
}

export function changeAdminReviewStatus(
  adminKey: string,
  reviewId: number,
  status: UpdateReviewStatusRequestStatus,
  expectedContentRevision: number,
  expectedVersion: number,
  reason?: string,
): Promise<AdminReviewResponse> {
  return updateAdminReviewStatus(
    reviewId,
    {
      status,
      reason: reason || null,
      expectedContentRevision,
      expectedVersion,
    },
    { headers: adminHeaders(adminKey) },
  );
}

export function fetchReviewModerationActions(
  adminKey: string,
  reviewId: number,
  signal?: AbortSignal,
): Promise<ReviewModerationActionResponse[]> {
  return listReviewModerationActions(
    reviewId,
    { headers: adminHeaders(adminKey), signal },
  );
}

export function saveOfficialReviewReply(
  adminKey: string,
  reviewId: number,
  content: string,
  expectedVersion: number,
): Promise<AdminReviewResponse> {
  return upsertOfficialReviewReply(
    reviewId,
    { content, expectedVersion },
    { headers: adminHeaders(adminKey) },
  );
}

export function removeOfficialReviewReply(
  adminKey: string,
  reviewId: number,
  expectedVersion: number,
): Promise<AdminReviewResponse> {
  return deleteOfficialReviewReply(
    reviewId,
    { expectedVersion },
    { headers: adminHeaders(adminKey) },
  );
}

export async function fetchAdminReviewEvidenceImage(
  adminKey: string,
  evidenceId: number,
  sortOrder: number,
  signal?: AbortSignal,
): Promise<Blob> {
  const blob = await getAdminReviewEvidenceImage(evidenceId, sortOrder, {
    headers: adminHeaders(adminKey),
    cache: "no-store",
    signal,
  });
  if (!blob.type.startsWith("image/")) {
    throw new Error("후기 증거 이미지 응답 형식이 올바르지 않습니다.");
  }
  return blob;
}

export async function fetchAdminReviewImage(
  adminKey: string,
  reviewId: number,
  imageId: number,
  signal?: AbortSignal,
): Promise<Blob> {
  const blob = await getAdminReviewImage(reviewId, imageId, {
    headers: adminHeaders(adminKey),
    cache: "no-store",
    signal,
  });
  if (!blob.type.startsWith("image/")) {
    throw new Error("숨김 후기 이미지 응답 형식이 올바르지 않습니다.");
  }
  return blob;
}

export function fetchAdminReviewReports(
  adminKey: string,
  filters: { status?: ReviewReportStatus; cursor?: string },
  signal?: AbortSignal,
): Promise<AdminReviewReportPageResponse> {
  return listAdminReviewReports(
    { ...filters, size: 20 },
    { headers: adminHeaders(adminKey), signal },
  );
}

export function fetchAdminReviewReport(
  adminKey: string,
  reportId: number,
  signal?: AbortSignal,
): Promise<AdminReviewReportResponse> {
  return getAdminReviewReport(
    reportId,
    { headers: adminHeaders(adminKey), signal },
  );
}

export function decideReviewReport(
  adminKey: string,
  reportId: number,
  decision: ReviewReportDecision,
  note?: string,
): Promise<AdminReviewReportResponse> {
  return decideAdminReviewReport(
    reportId,
    { decision, note: note?.trim() || null },
    { headers: adminHeaders(adminKey) },
  );
}
