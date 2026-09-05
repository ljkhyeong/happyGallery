import type { GroupInquirySummaryResponse } from "@/generated/api/customerStore";

export type GroupInquiryStatus = GroupInquirySummaryResponse["status"];
export const GROUP_INQUIRY_STATUS: Record<GroupInquiryStatus, string> = {
  RECEIVED: "접수", CONSULTING: "상담 중", CONFIRMED: "확정", CLOSED: "종료",
};
