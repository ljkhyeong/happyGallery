import type { ReviewReportReason } from "./api";

export const REVIEW_REPORT_REASON_LABELS: Record<ReviewReportReason, string> = {
  SPAM: "광고·도배",
  ABUSIVE: "욕설·비방",
  PRIVACY: "개인정보 노출",
  FALSE_INFORMATION: "허위 정보",
  OTHER: "기타",
};
