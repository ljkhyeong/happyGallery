import {
  decideAdminReviewReport,
  deleteOfficialReviewReply,
  listAdminReviewReports,
  listAdminReviews,
  listReviewModerationActions,
  upsertOfficialReviewReply,
  updateAdminReviewStatus,
  type AdminReviewReportPageResponse,
  type AdminReviewReportResponse,
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

export function changeAdminReviewStatus(
  adminKey: string,
  reviewId: number,
  status: UpdateReviewStatusRequestStatus,
  reason?: string,
): Promise<AdminReviewResponse> {
  return updateAdminReviewStatus(
    reviewId,
    { status, reason: reason || null },
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
): Promise<AdminReviewResponse> {
  return upsertOfficialReviewReply(
    reviewId,
    { content },
    { headers: adminHeaders(adminKey) },
  );
}

export function removeOfficialReviewReply(
  adminKey: string,
  reviewId: number,
): Promise<AdminReviewResponse> {
  return deleteOfficialReviewReply(
    reviewId,
    { headers: adminHeaders(adminKey) },
  );
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
